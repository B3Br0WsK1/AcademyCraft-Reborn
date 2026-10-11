package cn.lambdalib2.registry;

import net.minecraft.server.MinecraftServer;

/**
 * Lifecycle events passed to {@link StateEventCallback} methods. They replace FML's state events.
 * <ul>
 *     <li>{@link PreInit} — first stage, registries are open</li>
 *     <li>{@link Init} — main stage</li>
 *     <li>{@link PostInit} — everything is registered</li>
 *     <li>{@link ServerStarting} — a server (integrated or dedicated) is starting</li>
 *     <li>{@link ServerStopping} — a server is stopping</li>
 * </ul>
 * Callbacks marked client-only (with {@code @Environment(EnvType.CLIENT)} on the method or declaring class) are run
 *  by the client entrypoint, after all common callbacks of the same stage.
 */
public final class StateEvents {

    private StateEvents() {}

    public static class StateEvent {}

    public static final class PreInit extends StateEvent {}

    public static final class Init extends StateEvent {}

    public static final class PostInit extends StateEvent {}

    /**
     * Fired once (common callbacks only) after the game registries are frozen and item components are bound, i.e.
     *  when ItemStacks can be created. Fired before the first server starts, or when the client first joins a world
     *  (client-only callbacks are fired then as well).
     */
    public static final class RegistriesReady extends StateEvent {}

    public static final class ServerStarting extends StateEvent {
        public final MinecraftServer server;

        public ServerStarting(MinecraftServer server) {
            this.server = server;
        }

        public MinecraftServer getServer() {
            return server;
        }
    }

    public static final class ServerStopping extends StateEvent {
        public final MinecraftServer server;

        public ServerStopping(MinecraftServer server) {
            this.server = server;
        }
    }
}
