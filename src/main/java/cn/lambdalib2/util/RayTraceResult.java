package cn.lambdalib2.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A ray trace result in the shape of 1.12's RayTraceResult, which merged block and entity hits into one class.
 *  Kept to make porting the original ray-trace-heavy code straightforward.
 */
public class RayTraceResult {

    public enum Type { MISS, BLOCK, ENTITY }

    public Type typeOfHit;
    public Vec3 hitVec;
    public Direction sideHit;
    public Entity entityHit;
    private BlockPos blockPos;

    public RayTraceResult(Type type, Vec3 hitVec, Direction side, BlockPos pos) {
        this.typeOfHit = type;
        this.hitVec = hitVec;
        this.sideHit = side;
        this.blockPos = pos;
    }

    public RayTraceResult(Entity entity) {
        this(entity, entity.position());
    }

    public RayTraceResult(Entity entity, Vec3 hitVec) {
        this.typeOfHit = Type.ENTITY;
        this.entityHit = entity;
        this.hitVec = hitVec;
    }

    public static RayTraceResult fromVanilla(HitResult r) {
        if (r == null) return null;
        if (r instanceof EntityHitResult ehr) {
            return new RayTraceResult(ehr.getEntity(), ehr.getLocation());
        } else if (r instanceof BlockHitResult bhr) {
            return new RayTraceResult(bhr.getType() == HitResult.Type.MISS ? Type.MISS : Type.BLOCK,
                bhr.getLocation(), bhr.getDirection(), bhr.getBlockPos());
        }
        return new RayTraceResult(Type.MISS, r.getLocation(), Direction.UP, BlockPos.containing(r.getLocation()));
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public boolean isMiss() {
        return typeOfHit == Type.MISS;
    }

    public BlockHitResult toBlockHitResult() {
        if (typeOfHit == Type.BLOCK) {
            return new BlockHitResult(hitVec, sideHit, blockPos, false);
        }
        return BlockHitResult.miss(hitVec, sideHit == null ? Direction.UP : sideHit, blockPos == null ? BlockPos.containing(hitVec) : blockPos);
    }

    @Override
    public String toString() {
        return "RayTraceResult{type=" + typeOfHit + ", hitVec=" + hitVec + ", side=" + sideHit + ", entity=" + entityHit +
            ", pos=" + blockPos + "}";
    }
}
