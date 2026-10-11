package cn.lambdalib2.util.entityx;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public abstract class MotionHandler<T extends Entity> {

    public boolean isActive = true;

    boolean isDead = false;

    /**
     * The field is set by EntityX when added into it.
     */
    T target;
    /**
     * The field is set by EntityX when added into it.
     */
    EntityX entityX;

    public MotionHandler() {}

    public abstract String getID();

    public abstract void onStart();

    /**
     * OnUpdate events will only be sent as long as isActive set to true
     */
    public abstract void onUpdate();

    public void setDead() {
        isDead = true;
    }

    protected T getTarget() {
        return target;
    }

    protected EntityX getEntityX() {
        return entityX;
    }

    protected Level world() {
        return target.level();
    }

    public boolean isRemote() {
        return target.level().isClientSide();
    }

}
