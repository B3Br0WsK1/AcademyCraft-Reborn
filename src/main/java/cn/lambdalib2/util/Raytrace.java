package cn.lambdalib2.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static cn.lambdalib2.util.VecUtils.add;
import static cn.lambdalib2.util.VecUtils.multiply;

/**
 * A better wrap up for ray trace routines, supporting entity filtering, block filtering, and combined RayTrace of
 * blocks and entities. Also provided functions for fast implementation on entity looking traces.
 * @author WeAthFolD
 */
public class Raytrace {

    private static RayTraceResult miss(Vec3 pos) {
        return new RayTraceResult(RayTraceResult.Type.MISS, pos, Direction.UP, BlockPos.containing(pos));
    }

    /**
     * Perform a ray trace.
     * @param world The world to perform on
     * @param vec1 Start point
     * @param vec2 End point
     * @param entityPred The entity predicate
     * @param blockSel The block predicate
     * @return The trace result, never null (MISS if nothing hit)
     */
    public static RayTraceResult perform(Level world, Vec3 vec1, Vec3 vec2, Predicate<Entity> entityPred, IBlockSelector blockSel) {
        RayTraceResult
            mop1 = rayTraceEntities(world, vec1, vec2, entityPred),
            mop2 = rayTraceBlocks(world, vec1, vec2, blockSel);

        if(mop1 != null && mop2 != null) {
            double d1 = mop1.hitVec.distanceTo(vec1);
            double d2 = mop2.hitVec.distanceTo(vec1);
            return d1 <= d2 ? mop1 : mop2;
        }
        if(mop1 != null)
            return mop1;

        return mop2 == null ? miss(vec2) : mop2;
    }

    public static RayTraceResult perform(Level world, Vec3 vec1, Vec3 vec2, Predicate<Entity> entityPred) {
        return perform(world, vec1, vec2, entityPred, null);
    }

    public static RayTraceResult perform(Level world, Vec3 vec1, Vec3 vec2) {
        return perform(world, vec1, vec2, null, null);
    }

    public static Pair<Vec3, RayTraceResult> getLookingPos(LivingEntity living, double dist) {
        return getLookingPos(living, dist, null, null);
    }

    public static Pair<Vec3, RayTraceResult> getLookingPos(LivingEntity living, double dist,
                                                           Predicate<Entity> pred) {
        return getLookingPos(living, dist, pred, null);
    }

    public static Pair<Vec3, RayTraceResult> getLookingPos(Entity living, double dist,
                                                           Predicate<Entity> esel, IBlockSelector bsel) {
        RayTraceResult pos = traceLiving(living, dist, esel, bsel);
        Vec3 end = null;
        if(pos != null) {
            end = pos.hitVec;
            if(pos.entityHit != null)
                end = new Vec3(end.x, end.y + pos.entityHit.getEyeHeight() * 0.6, end.z);
        }
        if(end == null) {
            Vec3 lookingPos = living.getLookAngle();
            end = VecUtils.add(living.position(), VecUtils.multiply(lookingPos, dist));
        }

        return Pair.of(end, pos);
    }

    public static RayTraceResult rayTraceEntities(Level world, Vec3 vec1, Vec3 vec2, Predicate<? super Entity> selector) {
        Entity entity = null;
        Vec3 hit = null;
        AABB boundingBox = WorldUtils.getBoundingBox(vec1, vec2);
        List<Entity> list = world.getEntities((Entity) null, boundingBox.inflate(1.0D),
                selector == null ? e -> true : selector::test);
        double d0 = 0.0D;

        for (Entity entity1 : list) {
            if (!entity1.isPickable() || (selector != null && !selector.test(entity1)))
                continue;

            float f = 0.3F;
            AABB axisalignedbb = entity1.getBoundingBox().inflate(f);
            Optional<Vec3> mob1 = axisalignedbb.clip(vec1, vec2);
            if (axisalignedbb.contains(vec1)) {
                mob1 = Optional.of(vec1);
            }

            if (mob1.isPresent()) {
                double d1 = vec1.distanceTo(mob1.get());

                if (d1 < d0 || d0 == 0.0D) {
                    entity = entity1;
                    hit = mob1.get();
                    d0 = d1;
                }
            }
        }

        if (entity != null) {
            return new RayTraceResult(entity, hit);
        }
        return null;
    }

    private static RayTraceResult clipBlock(Level world, BlockPos pos, Vec3 start, Vec3 end) {
        BlockState state = world.getBlockState(pos);
        VoxelShape shape = state.getShape(world, pos);
        if (shape.isEmpty()) {
            shape = state.getCollisionShape(world, pos);
        }
        if (shape.isEmpty()) return null;
        BlockHitResult result = shape.clip(start, end, pos);
        if (result == null) return null;
        return new RayTraceResult(RayTraceResult.Type.BLOCK, result.getLocation(), result.getDirection(), result.getBlockPos());
    }

    /**
     * Mojang code with minor changes to support block filtering.
     */
    public static RayTraceResult rayTraceBlocks(Level world, Vec3 start, Vec3 end, IBlockSelector filter) {
        if(filter == null)
            filter = BlockSelectors.filNormal;

        Vec3 current = start;

        final int x2 = Mth.floor(end.x);
        final int y2 = Mth.floor(end.y);
        final int z2 = Mth.floor(end.z);

        int x1 = Mth.floor(current.x);
        int y1 = Mth.floor(current.y);
        int z1 = Mth.floor(current.z);
        {
            BlockPos pos = new BlockPos(x1, y1, z1);
            if (filter.accepts(world, x1, y1, z1, world.getBlockState(pos).getBlock())) {
                RayTraceResult result = clipBlock(world, pos, current, end);
                if (result != null) {
                    return result;
                }
            }
        }

        for (int i = 0; i < 200; ++i) {
            if (x1 == x2 && y1 == y2 && z1 == z2) {
                return null;
            }

            boolean moveX = true;
            boolean moveY = true;
            boolean moveZ = true;
            double nextX = 999.0D;
            double nextY = 999.0D;
            double nextZ = 999.0D;

            if (x2 > x1) {
                nextX = x1 + 1.0D;
            } else if (x2 < x1) {
                nextX = x1 + 0.0D;
            } else {
                moveX = false;
            }

            if (y2 > y1) {
                nextY = y1 + 1.0D;
            } else if (y2 < y1) {
                nextY = y1 + 0.0D;
            } else {
                moveY = false;
            }

            if (z2 > z1) {
                nextZ = z1 + 1.0D;
            } else if (z2 < z1) {
                nextZ = z1 + 0.0D;
            } else {
                moveZ = false;
            }

            double xFactor = 999.0D;
            double yFactor = 999.0D;
            double zFactor = 999.0D;
            double dx = end.x - current.x;
            double dy = end.y - current.y;
            double dz = end.z - current.z;

            if (moveX) {
                xFactor = (nextX - current.x) / dx;
            }
            if (moveY) {
                yFactor = (nextY - current.y) / dy;
            }
            if (moveZ) {
                zFactor = (nextZ - current.z) / dz;
            }
            byte side;

            if (xFactor < yFactor && xFactor < zFactor) {
                if (x2 > x1) {
                    side = 4;
                } else {
                    side = 5;
                }
                current = new Vec3(nextX, current.y + dy * xFactor, current.z + dz*xFactor);
            } else if (yFactor < zFactor) {
                if (y2 > y1) {
                    side = 0;
                } else {
                    side = 1;
                }
                current = new Vec3(current.x + dx * yFactor, nextY, current.z + dz * yFactor);
            } else {
                if (z2 > z1) {
                    side = 2;
                } else {
                    side = 3;
                }
                current = new Vec3(current.x + dx * zFactor, current.y + dy * zFactor, nextZ);
            }

            x1 = Mth.floor(current.x);
            if (side == 5) {
                --x1;
            }

            y1 = Mth.floor(current.y);
            if (side == 1) {
                --y1;
            }

            z1 = Mth.floor(current.z);
            if (side == 3) {
                --z1;
            }
            BlockPos pos = new BlockPos(x1, y1, z1);
            if (filter.accepts(world, x1, y1, z1, world.getBlockState(pos).getBlock())) {
                RayTraceResult result = clipBlock(world, pos, current, end);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    public static RayTraceResult traceLiving(Entity entity, double dist) {
        return traceLiving(entity, dist, null, null);
    }

    public static RayTraceResult traceLiving(Entity entity, double dist, Predicate<Entity> pred) {
        return traceLiving(entity, dist, pred, null);
    }

    /**
     * Performs a RayTrace starting from the target entity's eye towards its looking direction.
     * The trace will automatically ignore the target entity.
     */
    public static RayTraceResult traceLiving(Entity entity, double dist, Predicate<Entity> pred, IBlockSelector blockSel) {
        Vec3 v1 = entity.getEyePosition(1),
                v2 = add(v1, multiply(entity.getLookAngle(), dist));

        Predicate<Entity> exclude = EntitySelectors.exclude(entity);

        return perform(entity.level(), v1, v2, pred == null ? exclude : exclude.and(pred), blockSel);
    }

}
