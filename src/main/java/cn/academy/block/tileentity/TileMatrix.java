package cn.academy.block.tileentity;

import cn.academy.ACItems;
import cn.academy.ACTiles;
import cn.academy.energy.api.block.IWirelessMatrix;
import cn.academy.item.ItemMatrixCore;
import cn.lambdalib2.multiblock.IMultiTile;
import cn.lambdalib2.multiblock.InfoBlockMulti;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.TargetPoints;
import cn.lambdalib2.template.tile.ITickableTile;
import cn.lambdalib2.util.Side;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * @author WeAthFolD
 */
public class TileMatrix extends TileInventory implements IWirelessMatrix, IMultiTile, ITickableTile {

    // Client-only for display
    public int plateCount;

    int updateTicker;

    private String placerName = "";

    public TileMatrix(BlockPos pos, BlockState state) {
        super(ACTiles.MATRIX, pos, state, "wireless_matrix", 4);
    }

    public static boolean isMatrixCore(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemMatrixCore;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if(stack.isEmpty())
            return false;
        if (0 <= slot && slot <= 2) {
            return stack.getItem() == ACItems.constraint_plate;
        } else if (slot == 3) {
            return isMatrixCore(stack);
        } else {
            return false;
        }
    }

    public void setPlacer(Player player) {
        placerName = player.getName().getString();
        setChanged();
    }

    public String getPlacerName() {
        return placerName;
    }

    @Override
    public void update() {
        InfoBlockMulti info = getBlockInfo();

        if(info.getSubID() == 0) {
            if(!getWorld().isClientSide() && ++updateTicker == 15) {
                updateTicker = 0;
                this.sync();
            }
        }
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        placerName = nbt.getStringOr("placer", "");
    }

    @Override
    public CompoundTag writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putString("placer", placerName);
        return nbt;
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    /**
     * Server only.
     */
    public int getPlateCount() {
        int count = 0;
        for(int i = 0; i < 3; ++i) {
            if(!this.getStackInSlot(i).isEmpty())
                count++;
        }
        return count;
    }

    public int getCoreLevel() {
        ItemStack stack = getStackInSlot(3);
        return isMatrixCore(stack) ? ((ItemMatrixCore) stack.getItem()).level + 1 : 0;
    }

    @Override
    public int getCapacity() {
        return isWorking() ? 8 * getCoreLevel() : 0;
    }

    @Override
    public double getBandwidth() {
        int L = getCoreLevel();
        return isWorking() ? L * L * 60 : 0;
    }

    @Override
    public double getRange() {
        return isWorking() ? 24 * Math.sqrt(getCoreLevel()) : 0;
    }

    private boolean isWorking() {
        return getCoreLevel() > 0 && getPlateCount() == 3;
    }

    private void sync() {
        NetworkMessage.sendToAllAround(
                TargetPoints.convert(this, 25),
                this, "sync", getPlateCount(), placerName);
    }

    @NetworkMessage.Listener(channel="sync", side=Side.CLIENT)
    private void hSync(int plateCount2, String placerName2) {
        plateCount = plateCount2;
        placerName = placerName2;
    }
}
