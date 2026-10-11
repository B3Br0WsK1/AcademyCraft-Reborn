package cn.lambdalib2.util.entityx;

public abstract class EntityEventHandler<T extends EntityEvent> {

    public boolean active = true;

    public abstract Class<? extends EntityEvent> getHandledEvent();

    public abstract void onEvent(T event);
}
