package cn.academy.block.tileentity;

import cn.academy.ACItems;
import cn.academy.ACTiles;
import cn.academy.block.container.ContainerPhaseGen;
import cn.academy.energy.IFConstants;
import cn.academy.item.ItemMatterUnit;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.TargetPoints;
import cn.lambdalib2.util.Side;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * @author WeAthFolD
 */
public class TilePhaseGen extends TileGeneratorBase {

    // 废话~~~
    public static final int
        SLOT_LIQUID_IN = ContainerPhaseGen.SLOT_LIQUID_IN,
        SLOT_LIQUID_OUT = ContainerPhaseGen.SLOT_LIQUID_OUT,
        SLOT_OUTPUT = ContainerPhaseGen.SLOT_OUTPUT;

    static final int CONSUME_PER_TICK = 100;
    static final double GEN_PER_MB = 0.5;

    int untilSync;

    public TilePhaseGen(BlockPos pos, BlockState state) {
        super(ACTiles.PHASE_GEN, pos, state, "phase_gen", 3, 6000, IFConstants.LATENCY_MK1);
    }

    @StateEventCallback
    private static void registerStorage(StateEvents.Init ev) {
        FluidStorage.SIDED.registerForBlockEntity((tile, dir) -> tile.tank, ACTiles.PHASE_GEN);
    }

    @Override
    public double getGeneration(double required) {
        int maxDrain = (int) Math.min(CONSUME_PER_TICK, required / GEN_PER_MB);
        if (maxDrain <= 0)
            return 0;
        return tank.drainMB(maxDrain) * GEN_PER_MB;
    }

    @Override
    public void update() {
        super.update();

        if(!getWorld().isClientSide()) {
            if(++untilSync == 10) {
                untilSync = 0;
                sync();
            }

            ItemStack stack;
            { // Sink in liquid
                stack = getStackInSlot(SLOT_LIQUID_IN);

                if(!stack.isEmpty() && isPhaseLiquid(stack) && isOutputSlotAvailable() &&
                        (getTankSize() - getLiquidAmount() > PER_UNIT)) {

                    if(stack.getCount() > 0) {
                        tank.fillMB(PER_UNIT);
                        stack.shrink(1);
                    }

                    ItemStack output = getStackInSlot(SLOT_LIQUID_OUT);
                    if(!output.isEmpty()) {
                        output.grow(1);
                    } else {
                        this.setInventorySlotContents(SLOT_LIQUID_OUT,
                            ACItems.matter_unit.create(ItemMatterUnit.MAT_NONE));
                    }
                    setChanged();
                }
            }

            { // Output energy
                stack = getStackInSlot(SLOT_OUTPUT);
                if(!stack.isEmpty())
                    this.tryChargeStack(stack);
            }
        }
    }

    // Fluid handling
    static final int TANK_SIZE = 8000;
    static final int PER_UNIT = 1000;

    public final ImagPhaseTank tank = new ImagPhaseTank(TANK_SIZE, this::setChanged);

    public int getLiquidAmount() {
        return tank.getAmountMB();
    }

    public int getTankSize() {
        return tank.getCapacityMB();
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_LIQUID_IN)
            return isPhaseLiquid(stack);
        if (slot == SLOT_OUTPUT)
            return cn.academy.energy.api.IFItemManager.instance.isSupported(stack);
        return false;
    }

    @Override
    public void readFromNBT(CompoundTag tag) {
        super.readFromNBT(tag);
        tank.readFromNBT(tag);
    }

    @Override
    public CompoundTag writeToNBT(CompoundTag tag) {
        super.writeToNBT(tag);
        tank.writeToNBT(tag);
        return tag;
    }

    private void sync() {
        CompoundTag nbt = new CompoundTag();
        writeToNBT(nbt);
        NetworkMessage.sendToAllAround(TargetPoints.convert(this, 15), this, "MSG_INFO_SYNC", nbt);
    }

    @NetworkMessage.Listener(channel="MSG_INFO_SYNC", side= Side.CLIENT)
    private void onSync(CompoundTag nbt)
    {
        readFromNBT(nbt);
    }

    private boolean isPhaseLiquid(ItemStack stack) {
        return stack.getItem() == ACItems.matter_unit &&
                ACItems.matter_unit.getMaterial(stack) == ItemMatterUnit.MAT_PHASE_LIQUID;
    }

    private boolean isOutputSlotAvailable() {
        ItemStack stack = getStackInSlot(SLOT_LIQUID_OUT);
        return stack.isEmpty() || (stack.getItem() == ACItems.matter_unit &&
                ACItems.matter_unit.getMaterial(stack) == ItemMatterUnit.MAT_NONE && stack.getCount() < stack.getMaxStackSize());
    }

}
