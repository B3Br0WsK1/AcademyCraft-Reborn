package cn.lambdalib2.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Side-related utilities. The "runtime side" is resolved by the current thread, the same way as Forge's
 *  effective side: the client render thread is CLIENT, anything else is SERVER.
 */
public class SideUtils {

    /**
     * Hooks provided by the client environment. Installed by client initialization.
     */
    public interface ClientHooks {
        boolean isClientThread();
        Level getClientLevel();
        Player getClientPlayer();
    }

    private static ClientHooks clientHooks;
    private static MinecraftServer server;

    public static void _setClientHooks(ClientHooks hooks) {
        clientHooks = hooks;
    }

    public static void _setServer(MinecraftServer srv) {
        server = srv;
    }

    public static MinecraftServer getServer() {
        return server;
    }

    public static boolean isPhysicalClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    // API
    public static Side getRuntimeSide() {
        if (clientHooks != null && clientHooks.isClientThread()) {
            return Side.CLIENT;
        }
        return Side.SERVER;
    }

    public static boolean isClient() {
        return getRuntimeSide().isClient();
    }

    public static Level getWorld(ResourceKey<Level> dimension) {
        if (isClient()) {
            Level lvl = clientHooks.getClientLevel();
            return lvl != null && lvl.dimension().equals(dimension) ? lvl : null;
        } else {
            return server == null ? null : server.getLevel(dimension);
        }
    }

    public static Player getThePlayer() {
        if (isClient()) {
            return clientHooks.getClientPlayer();
        }
        return null;
    }

    public static Player findPlayerOnServer(String name) {
        if (isClient() || server == null) return null;
        return server.getPlayerList().getPlayerByName(name);
    }

    public static Player[] getPlayerListOnServer() {
        if (isClient() || server == null) return new Player[0];
        List<ServerPlayer> list = server.getPlayerList().getPlayers();
        return list.toArray(new Player[0]);
    }

}
