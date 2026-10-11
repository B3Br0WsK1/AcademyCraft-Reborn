package cn.lambdalib2.util;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class EntityLook {

    public float yaw, pitch;

    public EntityLook() {}

    public EntityLook(Entity e) {
        this(e.getYRot(), e.getXRot());
    }

    public EntityLook(Vec3 v) {
        yaw = -MathUtils.toDegrees((float) Math.atan2(v.x, v.z));
        pitch = -MathUtils.toDegrees((float) Math.atan2(v.y, Math.sqrt(v.x * v.x + v.z * v.z)));
    }

    public EntityLook(Direction facing) {
        switch (facing) {
            case DOWN:
                yaw = 0; pitch = 90;
                break;
            case UP:
                yaw = 0; pitch = -90;
                break;
            case EAST:
                yaw = 90; pitch = 0;
                break;
            case WEST:
                yaw = -90; pitch = 0;
                break;
            case SOUTH:
                yaw = 0; pitch = 0;
                break;
            case NORTH:
                yaw = 180; pitch = 0;
                break;
        }
    }

    public EntityLook(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Vec3 toVec3() {
        float yawRad = MathUtils.toRadians(yaw);
        float pitchRad = MathUtils.toRadians(pitch);
        return new Vec3(
            -Mth.sin(yawRad) * Mth.cos(pitchRad),
            -Mth.sin(pitchRad),
            Mth.cos(yawRad) * Mth.cos(pitchRad)
        );
    }

    public void applyToEntity(Entity e) {
        e.setYRot(yaw);
        e.setXRot(pitch);
    }
}
