package cn.lambdalib2.multiblock;

import cn.lambdalib2.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.core.Direction.*;

/**
 * A block made of several sub blocks. The orientation and the sub block index are stored in the block state, so every
 *  part knows where the origin (sub block 0) is. Only the origin has a block entity.
 * Subclasses must call {@link #finishInit()} at the end of their constructor.
 * @author WeathFolD
 */
public abstract class BlockMulti extends BaseEntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty SUB = IntegerProperty.create("sub", 0, 15);

    private final List<SubBlockPos> subList = new ArrayList<>();
    List<SubBlockPos>[] buffer;

    private final AABB[] renderBB = new AABB[6];

    double[][] rotCenters;

    boolean init = false;

    public static class SubBlockPos {
        public final int dx, dy, dz;

        public SubBlockPos(int _dx, int _dy, int _dz) {
            dx = _dx;
            dy = _dy;
            dz = _dz;
        }
    }

    public BlockMulti(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, NORTH).setValue(SUB, 0));
        addSubBlock(0, 0, 0);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SUB);
    }

    public void addSubBlock(int dx, int dy, int dz) {
        if (init) {
            throw new RuntimeException("Trying to add a sub block after block init finished");
        }
        if (subList.size() > 15) {
            throw new RuntimeException("Too many sub blocks");
        }
        subList.add(new SubBlockPos(dx, dy, dz));
    }

    /**
     * Accept a int[][3] array, add all the array element as a single subPos inside the list.
     */
    public void addSubBlock(int[][] data) {
        for (int[] s : data) {
            addSubBlock(s[0], s[1], s[2]);
        }
    }

    /**
     * You MUST call this via your ctor, after init all the blocks.
     */
    @SuppressWarnings("unchecked")
    public void finishInit() {
        buffer = new ArrayList[6];

        for (Direction dir : new Direction[] { NORTH, SOUTH, WEST, EAST }) {
            buffer[dir.ordinal()] = new ArrayList<>();
            for (SubBlockPos s : subList) {
                buffer[dir.ordinal()].add(rotate(s, dir));
            }
        }

        double[] arr = getRotCenter();
        rotCenters = new double[][] { {}, {}, { arr[0], arr[1], arr[2] }, { -arr[0], arr[1], -arr[2] },
                { arr[2], arr[1], -arr[0] }, { -arr[2], arr[1], arr[0] } };

        init = true;
    }

    public int getSubCount() {
        return subList.size();
    }

    public List<SubBlockPos> getRotatedSubList(Direction dir) {
        return buffer[dir.ordinal()];
    }

    /**
     * Get the render bounding box of this BlockMulti at the given block position (as origin block).
     */
    public AABB getRenderBB(BlockPos pos, Direction dir) {
        if (renderBB[dir.ordinal()] == null) {
            Vec3[] vecs = new Vec3[subList.size() * 2];
            for (int i = 0; i < subList.size(); ++i) {
                SubBlockPos rot = rotate(subList.get(i), dir);
                vecs[i * 2] = new Vec3(rot.dx, rot.dy, rot.dz);
                vecs[i * 2 + 1] = new Vec3(rot.dx + 1, rot.dy + 1, rot.dz + 1);
            }

            renderBB[dir.ordinal()] = WorldUtils.minimumBounds(vecs);
        }

        return renderBB[dir.ordinal()].move(pos);
    }

    // Rotation API
    private static final Direction[] rotMap = {
        NORTH, // -Z,
        EAST, // +X,
        SOUTH, // +Z,
        WEST // -X
    };

    private static final double[] drMap = { 0, 0, 180, 0, -90, 90 };
    private static final double[][] offsetMap = { { 0, 0 }, // placeholder
            { 0, 0 }, // placeholder
            { 0, 0 }, { 1, 1 }, { 0, 1 }, { 1, 0 } };

    public double[] getPivotOffset(InfoBlockMulti info) {
        return getPivotOffset(info.dir);
    }

    public Direction getRotation(int l) {
        return rotMap[l];
    }

    /**
     * Get the whole structure's (minX, minZ) point coord, in [dir = 0] (a.k.a: facing z-) point of view.
     */
    public double[] getPivotOffset(Direction dir) {
        return offsetMap[dir.ordinal()];
    }

    public double[] getRotCenter(Direction dir) {
        return rotCenters[dir.ordinal()];
    }

    /**
     * @return The rotation (in degrees around Y) used to render the structure.
     */
    public double getRotationDegrees(Direction dir) {
        return drMap[dir.ordinal()];
    }

    public abstract double[] getRotCenter();

    /**
     * Creates the block entity of the origin block.
     */
    public abstract BlockEntity createNewTileEntity(BlockPos pos, BlockState state);

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(SUB) == 0 ? createNewTileEntity(pos, state) : null;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /**
     * Build a multiblock at the given coordinate.
     */
    public void setMultiBlock(Level world, BlockPos pos, Direction dir) {
        world.setBlock(pos, defaultBlockState().setValue(FACING, dir), Block.UPDATE_ALL);
        placeSubBlocks(world, pos, dir);
    }

    public static Direction facingFromYaw(float yaw) {
        int l = Mth.floor(yaw * 4.0F / 360.0F + 0.5D) & 3;
        return rotMap[l];
    }

    // Placement API
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction dir = facingFromYaw(context.getRotation());
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        for (SubBlockPos s : buffer[dir.ordinal()]) {
            BlockPos npos = pos.offset(s.dx, s.dy, s.dz);
            if (!level.getBlockState(npos).canBeReplaced() && !npos.equals(pos)) {
                return null;
            }
            if (!level.isInWorldBounds(npos)) {
                return null;
            }
        }
        return defaultBlockState().setValue(FACING, dir).setValue(SUB, 0);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        placeSubBlocks(world, pos, state.getValue(FACING));
    }

    private void placeSubBlocks(Level world, BlockPos pos, Direction dir) {
        List<SubBlockPos> rotatedList = buffer[dir.ordinal()];
        for (int i = 1; i < rotatedList.size(); ++i) {
            SubBlockPos sub = rotatedList.get(i);
            BlockPos npos = pos.offset(sub.dx, sub.dy, sub.dz);
            world.setBlock(npos, defaultBlockState().setValue(FACING, dir).setValue(SUB, i), Block.UPDATE_ALL);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && state.getValue(SUB) != 0) {
            // Breaking any part breaks the origin, which drops the item.
            BlockPos origin = getOrigin(level, pos);
            if (origin != null) {
                level.destroyBlock(origin, !player.getAbilities().instabuild, player);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        if (!(state.getBlock() instanceof BlockMulti)) return;
        Direction dir = state.getValue(FACING);
        int sub = state.getValue(SUB);
        List<SubBlockPos> rotated = buffer[dir.ordinal()];
        if (sub >= rotated.size()) return;
        SubBlockPos self = rotated.get(sub);
        BlockPos origin = pos.offset(-self.dx, -self.dy, -self.dz);
        for (int i = 0; i < rotated.size(); ++i) {
            if (i == sub) continue;
            SubBlockPos s = rotated.get(i);
            BlockPos npos = origin.offset(s.dx, s.dy, s.dz);
            BlockState ns = level.getBlockState(npos);
            if (ns.getBlock() == this && ns.getValue(FACING) == dir && ns.getValue(SUB) == i) {
                level.removeBlock(npos, false);
            }
        }
    }

    // A series of getOrigin funcs.

    public @Nullable BlockPos getOrigin(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() != this) return null;
        Direction dir = state.getValue(FACING);
        int sub = state.getValue(SUB);
        List<SubBlockPos> rotated = buffer[dir.ordinal()];
        if (sub >= rotated.size()) return null;
        SubBlockPos sbp = rotated.get(sub);
        return pos.offset(-sbp.dx, -sbp.dy, -sbp.dz);
    }

    public @Nullable BlockPos getOrigin(BlockEntity te) {
        return te == null || te.getLevel() == null ? null : getOrigin(te.getLevel(), te.getBlockPos());
    }

    public @Nullable BlockEntity getOriginTile(Level world, BlockPos pos) {
        BlockPos origin = getOrigin(world, pos);
        if (origin == null) return null;
        BlockEntity te = world.getBlockEntity(origin);
        return te instanceof IMultiTile ? te : null;
    }

    public @Nullable BlockEntity getOriginTile(BlockEntity now) {
        return now == null || now.getLevel() == null ? null : getOriginTile(now.getLevel(), now.getBlockPos());
    }

    // Internal
    public static SubBlockPos rotate(SubBlockPos s, Direction dir) {
        switch (dir) {
        case EAST:
            return new SubBlockPos(-s.dz, s.dy, s.dx);
        case WEST:
            return new SubBlockPos(s.dz, s.dy, -s.dx);
        case SOUTH:
            return new SubBlockPos(-s.dx, s.dy, -s.dz);
        case NORTH:
            return new SubBlockPos(s.dx, s.dy, s.dz);
        default:
            throw new RuntimeException("Invalid rotate direction");
        }
    }

    double getRotation(InfoBlockMulti info) {
        return drMap[info.dir.ordinal()];
    }

}
