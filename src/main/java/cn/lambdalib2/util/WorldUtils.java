package cn.lambdalib2.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Predicate;

public class WorldUtils {

    /**
     * Judge whether a world is valid (e.g. currently in use), and false if world is null.
     */
    public static boolean isWorldValid(Level world) {
        if (world == null) {
            return false;
        }
        if (world.isClientSide()) {
            return SideUtils.isClient() && SideUtils.getThePlayer() != null && SideUtils.getThePlayer().level() == world;
        } else {
            MinecraftServer server = SideUtils.getServer();
            if (server == null) return false;
            for (ServerLevel w : server.getAllLevels()) {
                if (w == world) {
                    return true;
                }
            }
            return false;
        }
    }

    public static AABB getBoundingBox(Vec3 vec1, Vec3 vec2) {
        return new AABB(
            Math.min(vec1.x, vec2.x), Math.min(vec1.y, vec2.y), Math.min(vec1.z, vec2.z),
            Math.max(vec1.x, vec2.x), Math.max(vec1.y, vec2.y), Math.max(vec1.z, vec2.z));
    }

    /**
     * Return a minimum AABB that can hold the points given.
     */
    public static AABB minimumBounds(Vec3... points) {
        if (points.length == 0) {
            throw new RuntimeException("Invalid call: too few vectors");
        }

        double minX = points[0].x;
        double minY = points[0].y;
        double minZ = points[0].z;
        double maxX = points[0].x;
        double maxY = points[0].y;
        double maxZ = points[0].z;

        for (int i = 1; i < points.length; ++i) {
            minX = Math.min(minX, points[i].x);
            maxX = Math.max(maxX, points[i].x);
            minY = Math.min(minY, points[i].y);
            maxY = Math.max(maxY, points[i].y);
            minZ = Math.min(minZ, points[i].z);
            maxZ = Math.max(maxZ, points[i].z);
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static void getBlocksWithin(List<BlockPos> outList, Entity entity, double range, int max, IBlockSelector... filters) {
        getBlocksWithin(outList, entity.level(), entity.getX(), entity.getY(), entity.getZ(), range, max, filters);
    }

    public static void getBlocksWithin(List<BlockPos> outList, BlockEntity te, double range, int max, IBlockSelector... filters) {
        getBlocksWithin(outList, te.getLevel(), te.getBlockPos().getX() + 0.5, te.getBlockPos().getY() + 0.5, te.getBlockPos().getZ() + 0.5, range, max, filters);
    }

    public static void getBlocksWithin(
            List<BlockPos> outList,
            Level world,
            final double x, final double y, final double z,
            double range, int max,
            IBlockSelector... filter) {
        IBlockSelector[] fs = new IBlockSelector[filter.length + 1];
        System.arraycopy(filter, 0, fs, 0, filter.length);

        final double rangeSq = range * range;

        fs[filter.length] = (world1, xx, yy, zz, block) -> {
            double dx = xx - x, dy = yy - y, dz = zz - z;
            return dx * dx + dy * dy + dz * dz <= rangeSq;
        };

        int minX = Mth.floor(x - range),
                minY = Mth.floor(y - range),
                minZ = Mth.floor(z - range),
                maxX = Mth.ceil(x + range),
                maxY = Mth.ceil(y + range),
                maxZ = Mth.ceil(z + range);

        getBlocksWithin(outList, world, minX, minY, minZ, maxX, maxY, maxZ, max, fs);
    }

    public static void getBlocksWithin(
            List<BlockPos> outList,
            Level world,
            int minX, int minY, int minZ,
            int maxX, int maxY, int maxZ,
            int max,
            IBlockSelector... filter) {

        outList.clear();
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; ++x) {
            for (int y = minY; y <= maxY; ++y) {
                for (int z = minZ; z <= maxZ; ++z) {
                    boolean match = true;
                    Block block = world.getBlockState(mpos.set(x, y, z)).getBlock();
                    for (IBlockSelector f : filter) {
                        if (!f.accepts(world, x, y, z, block)) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        outList.add(new BlockPos(x, y, z));
                        if (outList.size() == max)
                            return;
                    }
                }
            }
        }
    }

    public static List<Entity> getEntities(BlockEntity te, double range, Predicate<Entity> predicate) {
        return getEntities(te.getLevel(), te.getBlockPos().getX() + 0.5, te.getBlockPos().getY() + 0.5, te.getBlockPos().getZ() + 0.5, range, predicate);
    }

    public static List<Entity> getEntities(Entity ent, double range, Predicate<Entity> predicate) {
        return getEntities(ent.level(), ent.getX(), ent.getY(), ent.getZ(), range, predicate);
    }

    public static List<Entity> getEntities(Level world, double x, double y, double z, double range,
                                           Predicate<Entity> filter) {
        AABB box = new AABB(
                x - range, y - range, z - range,
                x + range, y + range, z + range);
        Predicate<Entity> within = EntitySelectors.within(x, y, z, range);
        return getEntities(world, box, filter == null ? within : within.and(filter));
    }

    public static List<Entity> getEntities(Level world, AABB box, Predicate<Entity> predicate) {
        return world.getEntities((Entity) null, box, predicate == null ? e -> true : predicate::test);
    }

}
