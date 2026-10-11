package cn.academy.block.tileentity;

import cn.academy.ACItems;
import cn.academy.ACTiles;
import cn.academy.block.block.BlockImagFusor;
import cn.academy.client.sound.ACSounds;
import cn.academy.client.sound.PositionedSound;
import cn.academy.client.sound.TileEntitySound;
import cn.academy.crafting.ImagFusorRecipes;
import cn.academy.crafting.ImagFusorRecipes.IFRecipe;
import cn.academy.energy.IFConstants;
import cn.academy.item.ItemMatterUnit;
import cn.academy.support.EnergyItemHelper;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.TargetPoints;
import cn.lambdalib2.util.Side;
import cn.lambdalib2.util.StackUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * @author WeAthFolD
 */
public class TileImagFusor extends TileReceiverBase implements WorldlyContainer {

    static final double WORK_SPEED = 1.0 / 120;
    static final double CONSUME_PER_TICK = 12;
    static final int SYNC_INTV = 5;

    static final int TANK_SIZE = 8000;
    static final int PER_UNIT = 1000;

    //Inventory id:
    public static final int
        SLOT_INPUT = 0,
        SLOT_OUTPUT = 1,
        SLOT_IMAG_INPUT = 2,
        SLOT_ENERGY_INPUT = 3,
        SLOT_IMAG_OUTPUT = 4;

    private static final int[] slotsTop = {SLOT_INPUT, SLOT_IMAG_INPUT};
    private static final int[] slotsBottom = {SLOT_OUTPUT, SLOT_IMAG_OUTPUT, SLOT_ENERGY_INPUT};
    private static final int[] slotsOther = {SLOT_ENERGY_INPUT};

    /**
     * The imag phase liquid tank, exposed to other mods through the Fabric transfer API.
     */
    public final ImagPhaseTank tank = new ImagPhaseTank(TANK_SIZE, this::setChanged);

    private IFRecipe currentRecipe;
    private double workProgress;

    private int checkCooldown = 10, syncCooldown = SYNC_INTV;

    // Client-side synced values
    private int clientLiquid;
    private boolean clientWorking;

    public TileImagFusor(BlockPos pos, BlockState state) {
        super(ACTiles.IMAG_FUSOR, pos, state, "imag_fusor", 5, 2000, IFConstants.LATENCY_MK1);
    }

    @StateEventCallback
    private static void registerStorage(StateEvents.Init ev) {
        FluidStorage.SIDED.registerForBlockEntity((tile, dir) -> tile.tank, ACTiles.IMAG_FUSOR);
    }

    public int getTankSize() {
        return TANK_SIZE;
    }

    /**
     * @return Amount of liquid in the tank, in mB.
     */
    public int getLiquidAmount() {
        if (level != null && level.isClientSide())
            return clientLiquid;
        return tank.getAmountMB();
    }

    /**
     * As has discussed, this should perform a fake drain-energy effect when crafting, and restore to the real energy when not.
     * @return The energy for client-side display purpose.
     */
    public double getEnergyForDisplay() {
        return getEnergy();
    }

    @Override
    public void update() {
        super.update();

        boolean remote = level.isClientSide();

        if (!remote) {
            if (!isWorking()) {
                // Match the work in server
                if(--checkCooldown <= 0) {
                    checkCooldown = 10;
                    if(!inventory[SLOT_INPUT].isEmpty()) {
                        IFRecipe recipe = ImagFusorRecipes.INSTANCE.getRecipe(inventory[SLOT_INPUT]);
                        if(recipe != null) {
                            startWorking(recipe);
                        }
                    }
                }
            }

            if(isWorking()) {
                updateWork();
            }

            // Update liquid
            {
                ItemStack imagOutStack = inventory[SLOT_IMAG_OUTPUT];
                if(!inventory[SLOT_IMAG_INPUT].isEmpty() &&
                   (imagOutStack.isEmpty() || imagOutStack.getCount() < imagOutStack.getMaxStackSize()) &&
                    getLiquidAmount() + PER_UNIT <= TANK_SIZE) {

                    tank.fillMB(PER_UNIT);

                    inventory[SLOT_IMAG_INPUT].shrink(1);
                    if(inventory[SLOT_IMAG_INPUT].getCount() == 0)
                        inventory[SLOT_IMAG_INPUT] = ItemStack.EMPTY;

                    if (imagOutStack.isEmpty()) {
                        inventory[SLOT_IMAG_OUTPUT] = ACItems.matter_unit.create(ItemMatterUnit.MAT_NONE);
                    } else {
                        imagOutStack.grow(1);
                    }
                    setChanged();
                }
            }

            // Update energy
            if(!inventory[SLOT_ENERGY_INPUT].isEmpty()) {
                double gain = EnergyItemHelper
                        .pull(inventory[SLOT_ENERGY_INPUT], Math.min(getMaxEnergy() - getEnergy(), getBandwidth()), false);
                this.injectEnergy(gain);
            }

            updateFrame();

            if (--syncCooldown <= 0) {
                syncCooldown = SYNC_INTV;
                NetworkMessage.sendToAllAround(TargetPoints.convert(this, 15), this, "sync_work",
                    workProgress, getLiquidAmount(), isWorking() && !isActionBlocked());
            }
        } else {
            updateSounds();
        }
    }

    private void updateFrame() {
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof BlockImagFusor))
            return;
        int frame = 0;
        if (isWorking()) {
            frame = 1 + (int) ((level.getGameTime() / 8) % 4);
        }
        if (state.getValue(BlockImagFusor.FRAME) != frame) {
            level.setBlock(worldPosition, state.setValue(BlockImagFusor.FRAME, frame), Block.UPDATE_CLIENTS);
        }
    }

    @NetworkMessage.Listener(channel="sync_work", side=Side.CLIENT)
    private void hSyncWork(double progress, int liquid, boolean working) {
        workProgress = progress;
        clientLiquid = liquid;
        clientWorking = working;
    }

    //---Work API
    /**
     * @return The working progress, or 0.0 if isn't crafting
     */
    public double getWorkProgress() {
        if(!level.isClientSide())
            return isWorking() ? workProgress : 0.0;
        else
            return workProgress;
    }

    private void startWorking(IFRecipe recipe) {
        currentRecipe = recipe;
        workProgress = 0.0;
    }

    private void updateWork() {
        // Check the input stack, and abort if item isnt there
            // Also check whether the amount of Liquid is enough,
            // and whether the output of currentRecipe can be outputed into outputslot
            // Added by Shielian
        if(inventory[0].isEmpty() || currentRecipe.consumeType.getItem() != inventory[0].getItem()
                || this.pullEnergy(CONSUME_PER_TICK) != CONSUME_PER_TICK || this.getLiquidAmount() < currentRecipe.consumeLiquid
                || (!inventory[SLOT_OUTPUT].isEmpty() && inventory[SLOT_OUTPUT].getItem() != currentRecipe.output.getItem())
                ) {
            abortWorking();
            return;
        }

        if(!isActionBlocked()) {
            workProgress += WORK_SPEED;
            if(workProgress >= 1.0) {
                endWorking();
            }
        }
    }

    private void endWorking() {
        if(isWorking()) {
            tank.drainMB(currentRecipe.consumeLiquid);
            inventory[0].shrink(currentRecipe.consumeType.getCount());
            if(inventory[0].isEmpty())
                inventory[0]=ItemStack.EMPTY;

            if(!inventory[1].isEmpty()) {
                inventory[1].grow(currentRecipe.output.getCount());
            } else {
                inventory[1] = currentRecipe.output.copy();
            }
            setChanged();
        }

        workProgress = 0.0;
        currentRecipe = null;
        checkCooldown = 0; // Avoid work pausing
    }

    private void abortWorking() {
        workProgress = 0.0;
        currentRecipe = null ;
    }

    public boolean isWorking() {
        if (level != null && level.isClientSide())
            return clientWorking;
        return currentRecipe != null;
    }

    public boolean isActionBlocked() {
        if (level != null && level.isClientSide())
            return !clientWorking;
        return !isWorking() ||
            (inventory[0].getCount() < currentRecipe.consumeType.getCount()) ||
            (!inventory[1].isEmpty() && (!StackUtils.isStackDataEqual(inventory[1], currentRecipe.output) ||
            inventory[1].getCount() + currentRecipe.output.getCount() > inventory[1].getMaxStackSize())) ||
            currentRecipe.consumeLiquid > this.getLiquidAmount();
    }

    public IFRecipe getCurrentRecipe() {
        return currentRecipe;
    }

    //---

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        switch (slot) {
            case SLOT_INPUT:
                return ImagFusorRecipes.INSTANCE.getRecipe(stack) != null;
            case SLOT_IMAG_INPUT:
                return stack.getItem() == ACItems.matter_unit &&
                    ACItems.matter_unit.getMaterial(stack) == ItemMatterUnit.MAT_PHASE_LIQUID;
            case SLOT_ENERGY_INPUT:
                return EnergyItemHelper.isSupported(stack);
            default:
                return false;
        }
    }

    @Override
    public void readFromNBT(CompoundTag tag) {
        super.readFromNBT(tag);
        tank.readFromNBT(tag);
        int curRcp = tag.getIntOr("_work_recipe", -1);
        if(curRcp < 0 || curRcp >= ImagFusorRecipes.INSTANCE.getAllRecipe().size())
            currentRecipe=null;
        else
            currentRecipe= ImagFusorRecipes.INSTANCE.getAllRecipe().get(curRcp);
        workProgress = tag.getDoubleOr("_work_progress", 0);
    }

    @Override
    public CompoundTag writeToNBT(CompoundTag tag) {
        super.writeToNBT(tag);
        tank.writeToNBT(tag);
        tag.putInt("_work_recipe", currentRecipe==null?-1:currentRecipe.getID());
        tag.putDouble("_work_progress", workProgress);
        return tag;
    }

    // --- CLIENT EFFECTS

    @Environment(EnvType.CLIENT)
    private PositionedSound sound;

    @Environment(EnvType.CLIENT)
    private void updateSounds() {
        boolean play = !isActionBlocked();

        if(sound != null && !play) {
            sound.stop();
            sound = null;
        } else if(sound == null && play) {
            sound = new TileEntitySound(this, "machine.imag_fusor_work").setLoop().setVolume(0.6f);
            ACSounds.playClient(sound);
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        switch(side) {
            case DOWN:
                return slotsBottom;
            case UP:
                return slotsTop;
            default:
                return slotsOther;
        }
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack item, @Nullable Direction facing) {
        return this.isItemValidForSlot(slot, item);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack item, Direction facing) {
        return facing == Direction.DOWN;
    }
}
