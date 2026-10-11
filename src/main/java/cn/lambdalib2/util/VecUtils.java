package cn.lambdalib2.util;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class VecUtils {
    private static final Random rand = new Random();

    public static Vec3 random() {
        return _vec(-1 + 2 * rand.nextDouble(), -1 + 2 * rand.nextDouble(), -1 + 2 * rand.nextDouble());
    }

    public static Vec3 toDirVector(Entity ent) {
        return toDirVector(ent, 1.0f);
    }

    public static Vec3 toDirVector(Entity ent, float partialTicks) {
        return toDirVector(
            MathUtils.lerpDegree(ent.yRotO, ent.getYRot(), partialTicks),
            MathUtils.lerpDegree(ent.xRotO, ent.getXRot(), partialTicks)
        );
    }

    /**
     * Convert the yaw and pitch angle to the looking direction vector
     * @param yaw in mc entity angle space
     * @param pitch in mc entity angle space
     * @return the looking direction vec, normalized
     */
    public static Vec3 toDirVector(float yaw, float pitch) {
        float f1 = Mth.cos(-yaw * 0.017453292F - (float) Math.PI);
        float f2 = Mth.sin(-yaw * 0.017453292F - (float) Math.PI);
        float f3 = -Mth.cos(-pitch * 0.017453292F);
        float f4 = Mth.sin(-pitch * 0.017453292F);
        return new Vec3((double)(f2 * f3), (double)f4, (double)(f1 * f3));
    }

    /**
     * Get the closest point on line AB to point P.
     */
    public static Vec3 getClosestPointOn(Vec3 p, Vec3 a, Vec3 b) {
        double x0 = a.x, y0 = a.y, z0 = a.z,
                x1 = b.x, y1 = b.y, z1 = b.z;
        double X = p.x, Y = p.y, Z = p.z;

        double dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;

        double mid1 = dx * dx + dy * dy + dz * dz;
        double mid2 = dx * (x1 - X) + dy * (y1 - Y) + dz * (z1 - Z);

        double lambda = mid2 / mid1;

        return lerp(a, b, lambda);
    }

    public static Vec3 multiply(Vec3 v, double scale) {
        return new Vec3(v.x * scale, v.y * scale, v.z * scale);
    }

    public static Vec3 lerp(Vec3 a, Vec3 b, double lambda) {
        return new Vec3(
                MathUtils.lerp(a.x, b.x, lambda),
                MathUtils.lerp(a.y, b.y, lambda),
                MathUtils.lerp(a.z, b.z, lambda));
    }

    public static Vec3 neg(Vec3 v) {
        return new Vec3(-v.x, -v.y, -v.z);
    }

    public static Vec3 add(Vec3 a, Vec3 b) {
        return new Vec3(a.x + b.x, a.y + b.y, a.z + b.z);
    }

    public static Vec3 subtract(Vec3 a, Vec3 b) {
        return add(a, neg(b));
    }

    public static double magnitudeSq(Vec3 a) {
        return a.x * a.x + a.y * a.y + a.z * a.z;
    }

    public static double magnitude(Vec3 a) {
        return Math.sqrt(magnitudeSq(a));
    }

    public static Vec3 copy(Vec3 v) {
        return new Vec3(v.x, v.y, v.z);
    }

    public static Vec3 crossProduct(Vec3 a, Vec3 b) {
        double
            x0 = a.x, y0 = a.y, z0 = a.z,
            x1 = b.x, y1 = b.y, z1 = b.z;
        return new Vec3(
            y0 * z1 - y1 * z0,
            x1 * z0 - x0 * z1,
            x0 * y1 - x1 * y0);
    }

    // CREDITS TO Greg S for the original code.
    private static Vec3 getIntersection(double fDst1, double fDst2, Vec3 P1, Vec3 P2) {
        if ( (fDst1 * fDst2) >= 0.0f) return null;
        if ( fDst1 == fDst2) return null;
        return add(P1, multiply(subtract(P2, P1), ( -fDst1 / (fDst2-fDst1) )));
    }

    private static boolean inBox(Vec3 Hit, Vec3 B1, Vec3 B2, int Axis) {
        if ( Axis==1 && Hit.z > B1.z && Hit.z < B2.z && Hit.y > B1.y && Hit.y < B2.y) return true;
        if ( Axis==2 && Hit.z > B1.z && Hit.z < B2.z && Hit.x > B1.x && Hit.x < B2.x) return true;
        if ( Axis==3 && Hit.x > B1.x && Hit.x < B2.x && Hit.y > B1.y && Hit.y < B2.y) return true;
        return false;
    }

    public static Vec3 checkLineAABB(Vec3 L1, Vec3 L2, AABB aabb) {
        return checkLineBox(_vec(aabb.minX, aabb.minY, aabb.minZ),
                _vec(aabb.maxX, aabb.maxY, aabb.maxZ),
                L1, L2);
    }

    /**
     * Check if the line segment (L1, L2) intersects with AABB represented by (B1, B2).
     * If intersected, return the a hit point of the segment to the line.
     * Else, return null.
     */
    public static Vec3 checkLineBox(Vec3 B1, Vec3 B2, Vec3 L1, Vec3 L2) {
        if (L2.x < B1.x && L1.x < B1.x) return null;
        if (L2.x > B2.x && L1.x > B2.x) return null;
        if (L2.y < B1.y && L1.y < B1.y) return null;
        if (L2.y > B2.y && L1.y > B2.y) return null;
        if (L2.z < B1.z && L1.z < B1.z) return null;
        if (L2.z > B2.z && L1.z > B2.z) return null;

        if (L1.x > B1.x && L1.x < B2.x &&
            L1.y > B1.y && L1.y < B2.y &&
            L1.z > B1.z && L1.z < B2.z)
            return L1;

        Vec3 Hit;
        if ( ((Hit = getIntersection(L1.x-B1.x, L2.x-B1.x, L1, L2)) != null && inBox( Hit, B1, B2, 1 ))
          || ((Hit = getIntersection( L1.y-B1.y, L2.y-B1.y, L1, L2)) != null && inBox( Hit, B1, B2, 2 ))
          || ((Hit = getIntersection( L1.z-B1.z, L2.z-B1.z, L1, L2)) != null && inBox( Hit, B1, B2, 3 ))
          || ((Hit = getIntersection( L1.x-B2.x, L2.x-B2.x, L1, L2)) != null && inBox( Hit, B1, B2, 1 ))
          || ((Hit = getIntersection( L1.y-B2.y, L2.y-B2.y, L1, L2)) != null && inBox( Hit, B1, B2, 2 ))
          || ((Hit = getIntersection( L1.z-B2.z, L2.z-B2.z, L1, L2)) != null && inBox( Hit, B1, B2, 3 )))
            return Hit;

        return null;
    }

    public static Vec3 entityPos(Entity e) {
        return e.position();
    }

    public static Vec3 entityHeadPos(Entity e) {
        return e.getEyePosition(1f);
    }

    public static Vec3 entityMotion(Entity e) {
        return e.getDeltaMovement();
    }

    private static Vec3 _vec(double x, double y, double z) {
        return new Vec3(x, y, z);
    }

    public static Vec3 lookingPos(Entity e, double range){
        return add(e.getEyePosition(1F), multiply(e.getLookAngle(), range));
    }

    public static Vec3 rotateAroundZ(Vec3 v, float angle) {
        float f1 = Mth.cos(angle);
        float f2 = Mth.sin(angle);
        double d0 = v.x * (double)f1 + v.y * (double)f2;
        double d1 = v.y * (double)f1 - v.x * (double)f2;
        double d2 = v.z;
        return new Vec3(d0, d1, d2);
    }

    public static void setMotion(Entity e, Vec3 motion){
        e.setDeltaMovement(motion);
    }

    public static double getYaw(Vec3 v){
        return -(Math.atan2(v.x, v.z) * 180.0D / 3.141592653589793D);
    }

    public static double getPitch(Vec3 v){
        return -(Math.atan2(v.y, Math.sqrt(v.x * v.x + v.z * v.z)) * 180.0D / 3.141592653589793D);
    }
}
