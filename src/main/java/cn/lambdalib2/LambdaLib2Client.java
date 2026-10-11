package cn.lambdalib2;

import cn.lambdalib2.event.EventHub;
import cn.lambdalib2.event.mc.TickEvent;
import cn.lambdalib2.registry.RegistryManager;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.s11n.network.Future;
import cn.lambdalib2.s11n.network.LLNetwork;
import cn.lambdalib2.util.SideUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Client side initialization of LambdaLib2.
 */
@Environment(EnvType.CLIENT)
public final class LambdaLib2Client {

    private static boolean initialized = false;

    private LambdaLib2Client() {}

    /**
     * Installs the client hooks. Must be called as early as possible (before any side-sensitive code runs).
     */
    public static void installHooks() {
        cn.lambdalib2.util.GameTimer._setPauseHook(() -> Minecraft.getInstance().isPaused());
        SideUtils._setClientHooks(new SideUtils.ClientHooks() {
            @Override
            public boolean isClientThread() {
                Minecraft mc = Minecraft.getInstance();
                return mc != null && mc.isSameThread();
            }

            @Override
            public Level getClientLevel() {
                return Minecraft.getInstance().level;
            }

            @Override
            public Player getClientPlayer() {
                return Minecraft.getInstance().player;
            }
        });
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        cn.lambdalib2.render.LLPipelines.init();
        cn.lambdalib2.render.legacy.LegacyGL.init();
        cn.lambdalib2.auxgui.AuxGuiHandler.init();

        LLNetwork._setClientSender(ClientPlayNetworking::send);
        ClientPlayNetworking.registerGlobalReceiver(LLNetwork.Payload.TYPE, (payload, ctx) -> LLNetwork.dispatch(payload, null));

        ClientTickEvents.START_CLIENT_TICK.register(mc -> EventHub.BUS.post(new TickEvent.ClientTickEvent(TickEvent.Phase.START)));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> EventHub.BUS.post(new TickEvent.ClientTickEvent(TickEvent.Phase.END)));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> {
            Future.onDisconnect();
            EventHub.BUS.post(new cn.lambdalib2.event.mc.ClientDisconnectionEvent());
        });
    }

    private static boolean clientRegistriesReadyFired = false;

    public static void runClientStages() {
        // Item components are only bound once a world is joined (they're data driven in 26.x)
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register(
            (handler, sender, client) -> client.execute(() -> {
                RegistryManager.INSTANCE.fireRegistriesReady();
                if (!clientRegistriesReadyFired) {
                    clientRegistriesReadyFired = true;
                    RegistryManager.INSTANCE.fireClient(new StateEvents.RegistriesReady());
                }
            }));
        RegistryManager.INSTANCE.fireClient(new StateEvents.PreInit());
        RegistryManager.INSTANCE.fireClient(new StateEvents.Init());
        RegistryManager.INSTANCE.fireClient(new StateEvents.PostInit());
    }
}
