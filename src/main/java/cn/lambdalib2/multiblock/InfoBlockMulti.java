package cn.lambdalib2.multiblock;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Orientation & sub block index of a {@link BlockMulti} part. Derived from the block state.
 * @author WeathFolD
 */
public class InfoBlockMulti {

    final Direction dir;
    final int subID;

    public InfoBlockMulti(Direction dir, int subID) {
        this.dir = dir;
        this.subID = subID;
    }

    public static InfoBlockMulti of(BlockState state) {
        if (state.hasProperty(BlockMulti.FACING) && state.hasProperty(BlockMulti.SUB)) {
            return new InfoBlockMulti(state.getValue(BlockMulti.FACING), state.getValue(BlockMulti.SUB));
        }
        return new InfoBlockMulti(Direction.NORTH, 0);
    }

    public static InfoBlockMulti of(BlockEntity te) {
        return of(te.getBlockState());
    }

    /**
     * Always loaded, since the info is stored in the block state.
     */
    public boolean isLoaded() {
        return true;
    }

    public int getSubID() {
        return subID;
    }

    public Direction getDir() {
        return dir;
    }

}
