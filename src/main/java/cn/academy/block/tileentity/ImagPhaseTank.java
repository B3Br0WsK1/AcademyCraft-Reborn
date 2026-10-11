package cn.academy.block.tileentity;

import cn.academy.block.fluid.ACFluids;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.nbt.CompoundTag;

/**
 * An imag phase liquid tank (replacement of the Forge FluidTank), with millibucket based helpers for internal use.
 *  Exposed to other mods through {@link net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage}.
 */
public class ImagPhaseTank extends SingleVariantStorage<FluidVariant> {

    /** Droplets per millibucket. */
    public static final long DROPLETS_PER_MB = FluidConstants.BUCKET / 1000;

    private final int capacityMB;
    private final Runnable onChange;

    public ImagPhaseTank(int capacityMB, Runnable onChange) {
        this.capacityMB = capacityMB;
        this.onChange = onChange;
    }

    @Override
    protected FluidVariant getBlankVariant() {
        return FluidVariant.blank();
    }

    @Override
    protected long getCapacity(FluidVariant variant) {
        return capacityMB * DROPLETS_PER_MB;
    }

    @Override
    protected boolean canInsert(FluidVariant variant) {
        return variant.isOf(ACFluids.IMAG_PHASE);
    }

    @Override
    protected void onFinalCommit() {
        onChange.run();
    }

    public int getCapacityMB() {
        return capacityMB;
    }

    public int getAmountMB() {
        return (int) (amount / DROPLETS_PER_MB);
    }

    public void setAmountMB(int mB) {
        amount = Math.max(0, Math.min(capacityMB, mB)) * DROPLETS_PER_MB;
        variant = amount > 0 ? FluidVariant.of(ACFluids.IMAG_PHASE) : FluidVariant.blank();
    }

    /**
     * @return Amount actually filled, in mB
     */
    public int fillMB(int mB) {
        int before = getAmountMB();
        setAmountMB(before + mB);
        onChange.run();
        return getAmountMB() - before;
    }

    /**
     * @return Amount actually drained, in mB
     */
    public int drainMB(int mB) {
        int before = getAmountMB();
        setAmountMB(before - mB);
        onChange.run();
        return before - getAmountMB();
    }

    public void readFromNBT(CompoundTag tag) {
        setAmountMB(tag.getIntOr("liquid", 0));
    }

    public void writeToNBT(CompoundTag tag) {
        tag.putInt("liquid", getAmountMB());
    }

}
