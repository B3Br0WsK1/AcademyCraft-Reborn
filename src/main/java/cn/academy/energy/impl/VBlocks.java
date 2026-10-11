package cn.academy.energy.impl;

import cn.academy.energy.api.block.*;
import cn.lambdalib2.util.MathUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Virtual (position based) references to wireless tiles.
 * @author WeAthFolD
 */
public class VBlocks {

    static abstract class VBlock<T extends IWirelessTile> {

        protected final int x, y, z;
        protected final boolean ignoreChunk;

        public VBlock(BlockEntity te, boolean _ignoreChunk) {
            x = te.getBlockPos().getX();
            y = te.getBlockPos().getY();
            z = te.getBlockPos().getZ();
            ignoreChunk = _ignoreChunk;
        }

        public VBlock(CompoundTag tag, boolean _ignoreChunk) {
            x = tag.getIntOr("x", 0);
            y = tag.getIntOr("y", 0);
            z = tag.getIntOr("z", 0);
            ignoreChunk = _ignoreChunk;
        }

        public double distSq(VBlock another) {
            return MathUtils.distanceSq(another.x, another.y, another.z, x, y, z);
        }

        public BlockPos pos() {
            return new BlockPos(x, y, z);
        }

        public boolean isLoaded(Level world) {
            return world != null && world.isLoaded(pos());
        }

        @SuppressWarnings("unchecked")
        public T get(Level world) {
            if(world == null || !isLoaded(world))
                return null;

            BlockEntity te = world.getBlockEntity(pos());
            if(te == null || !isAcceptable(te)) {
                return null;
            }
            return (T) te;
        }

        public CompoundTag toNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("x", x);
            tag.putInt("y", y);
            tag.putInt("z", z);
            return tag;
        }

        @Override
        public int hashCode() {
            return x ^ y ^ z;
        }

        @Override
        public boolean equals(Object obj) {
            if(obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            VBlock vb = (VBlock) obj;
            return vb.x == x && vb.y == y && vb.z == z;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + x + ", " + y + ", " + z + "]";
        }

        protected abstract boolean isAcceptable(BlockEntity tile);

    }

    static class VWMatrix extends VBlock<IWirelessMatrix> {

        public VWMatrix(IWirelessMatrix te) {
            super((BlockEntity) te, true);
        }

        public VWMatrix(CompoundTag tag) {
            super(tag, true);
        }

        @Override
        protected boolean isAcceptable(BlockEntity tile) {
            return tile instanceof IWirelessMatrix;
        }

    }

    static class VWNode extends VBlock<IWirelessNode> {

        public VWNode(IWirelessNode te) {
            super((BlockEntity) te, false);
        }

        public VWNode(CompoundTag tag) {
            super(tag, false);
        }

        @Override
        protected boolean isAcceptable(BlockEntity tile) {
            return tile instanceof IWirelessNode;
        }

    }

    static class VNNode extends VBlock<IWirelessNode> {
        public VNNode(IWirelessNode te) {
            super((BlockEntity) te, true);
        }

        public VNNode(CompoundTag tag) {
            super(tag, true);
        }

        @Override
        protected boolean isAcceptable(BlockEntity tile) {
            return tile instanceof IWirelessNode;
        }
    }

    static class VNGenerator extends VBlock<IWirelessGenerator> {
        public VNGenerator(IWirelessGenerator te) {
            super((BlockEntity) te, true);
        }

        public VNGenerator(CompoundTag tag) {
            super(tag, true);
        }

        @Override
        protected boolean isAcceptable(BlockEntity tile) {
            return tile instanceof IWirelessGenerator;
        }
    }

    static class VNReceiver extends VBlock<IWirelessReceiver> {
        public VNReceiver(IWirelessReceiver te) {
            super((BlockEntity) te, true);
        }

        public VNReceiver(CompoundTag tag) {
            super(tag, true);
        }

        @Override
        protected boolean isAcceptable(BlockEntity tile) {
            return tile instanceof IWirelessReceiver;
        }
    }

}
