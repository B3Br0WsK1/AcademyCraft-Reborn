package cn.lambdalib2;

import cn.lambdalib2.event.EventHub;
import cn.lambdalib2.event.mc.LivingEvents;
import cn.lambdalib2.event.mc.PlayerEvents;
import cn.lambdalib2.event.mc.TickEvent;
import cn.lambdalib2.registry.AnnotationScanner;
import cn.lambdalib2.registry.RegistryManager;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.s11n.network.Future;
import cn.lambdalib2.s11n.network.LLNetwork;
import cn.lambdalib2.s11n.network.NetworkEvent;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkS11n;
import cn.lambdalib2.util.SideUtils;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LambdaLib2, ported to Fabric as an embedded library of AcademyCraft. Call {@link #init(String...)} from the main
 *  entrypoint and {@code LambdaLib2Client.init()} from the client entrypoint.
 */
public final class LambdaLib2 {

    public static final String MODID = "lambdalib2";
    public static final String VERSION = "0.2.0-fabric";

    /**
     * Whether we are in development (debug) mode.
     */
    public static final boolean DEBUG = FabricLoader.getInstance().isDevelopmentEnvironment();

    private static final Logger log = LoggerFactory.getLogger("LambdaLib2");

    private static boolean initialized = false;

    public static Logger getLogger() {
        return log;
    }

    private LambdaLib2() {}

    /**
     * Common initialization: scans the given mods for annotations, sets up networking and event bridges.
     */
    public static void init(String... modIds) {
        if (initialized) return;
        initialized = true;

        AnnotationScanner.scan(modIds);

        LLNetwork.init();
        NetworkMessage.init();
        NetworkEvent.init();
        Future.init();
        NetworkS11n.registerAll();

        // Server lifecycle
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            RegistryManager.INSTANCE.fireRegistriesReady();
            SideUtils._setServer(server);
            RegistryManager.INSTANCE.fireCommon(new StateEvents.ServerStarting(server));
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server ->
            RegistryManager.INSTANCE.fireCommon(new StateEvents.ServerStopping(server)));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            SideUtils._setServer(null);
            Future.onDisconnect();
        });

        // Ticks
        ServerTickEvents.START_SERVER_TICK.register(server ->
            EventHub.BUS.post(new TickEvent.ServerTickEvent(server, TickEvent.Phase.START)));
        ServerTickEvents.END_SERVER_TICK.register(server ->
            EventHub.BUS.post(new TickEvent.ServerTickEvent(server, TickEvent.Phase.END)));

        // Living events
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
            !EventHub.BUS.post(new LivingEvents.LivingAttackEvent(entity, source, amount)));
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) ->
            !EventHub.BUS.post(new LivingEvents.LivingDeathEvent(entity, source)));

        // Player events
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) ->
            EventHub.BUS.post(new PlayerEvents.Clone(newPlayer, oldPlayer, !alive)));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
            EventHub.BUS.post(new PlayerEvents.PlayerLoggedInEvent(handler.getPlayer())));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            EventHub.BUS.post(new PlayerEvents.PlayerLoggedOutEvent(handler.getPlayer()));
        });
    }

    /**
     * Runs the common load stages (PreInit, Init, PostInit).
     */
    public static void runCommonStages() {
        RegistryManager.INSTANCE.fireCommon(new StateEvents.PreInit());
        RegistryManager.INSTANCE.fireCommon(new StateEvents.Init());
        RegistryManager.INSTANCE.fireCommon(new StateEvents.PostInit());
    }

}
