package cn.academy.block.tileentity;

import cn.academy.energy.api.block.IWirelessReceiver;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.TargetPoints;
import cn.lambdalib2.template.tile.ITickableTile;
import cn.lambdalib2.util.Side;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BaseClass that should be used on all energy receivers.
 * This class will automatically sync its energy field to client side.
 * @author WeAthFolD
 */
public class TileReceiverBase extends TileInventory implements IWirelessReceiver, ITickableTile {

    private static final int UPDATE_WAIT = 20;

    int updateTicker = 0;

    final double maxEnergy;
    final double bandwidth;

    public double energy;

    public TileReceiverBase(BlockEntityType<?> type, BlockPos pos, BlockState state,
                            String name, int invSize, double max, double bwidth) {
        super(type, pos, state, name, invSize);
        maxEnergy = max;
        bandwidth = bwidth;
    }

    @Override
    public void update() {
        if(!getLevel().isClientSide()) {
            if(++updateTicker == UPDATE_WAIT) {
                updateTicker = 0;
                NetworkMessage.sendToAllAround(TargetPoints.convert(this, 15), this, ".tile.sync", writeToNBT(new CompoundTag()));
            }
        }
    }

    @Override
    public double getRequiredEnergy() {
        return maxEnergy - energy;
    }

    @Override
    public double injectEnergy(double amt) {
        double req = maxEnergy - energy;
        double give = Math.min(amt, req);
        energy += give;
        return amt - give;
    }

    public double getEnergy() {
        return energy;
    }

    public double getMaxEnergy() {
        return maxEnergy;
    }

    @Override
    public double getBandwidth() {
        return bandwidth;
    }

    @Override
    public void readFromNBT(CompoundTag tag) {
        super.readFromNBT(tag);
        energy = tag.getDoubleOr("energy", 0d);
    }

    @Override
    public CompoundTag writeToNBT(CompoundTag tag) {
        super.writeToNBT(tag);
        tag.putDouble("energy", energy);
        return tag;
    }

    @Listener(channel=".tile.sync", side=Side.CLIENT)
    private void hSync(CompoundTag nbt) {
        readFromNBT(nbt);
    }

    @Override
    public double pullEnergy(double amt) {
        double a = Math.min(amt, energy);
        energy -= a;
        return a;
    }

}
