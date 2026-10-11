package cn.lambdalib2.event;

/**
 * Holder of the global event bus, the counterpart of MinecraftForge.EVENT_BUS.
 */
public final class EventHub {
    public static final EventBus BUS = new EventBus();

    private EventHub() {}
}
