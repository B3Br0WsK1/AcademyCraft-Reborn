package cn.academy;

import cn.lambdalib2.event.EventHub;
import cn.lambdalib2.event.SubscribeEvent;
import cn.lambdalib2.event.mc.PlayerEvents;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.NetworkS11n;
import cn.lambdalib2.util.ResourceUtils;
import cn.lambdalib2.util.Side;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * The ability balance data config (HOCON), "academy-craft-data.conf". The server's config is sent to clients on login.
 */
public final class ACConfig {

    private ACConfig() {}

    private static Config config;

    private static String lastError = null;

    private static void __init() {
        Logger log = AcademyCraft.log;

        Identifier defaultRes = Identifier.parse("academy:config/default.conf");

        Reader reader = new InputStreamReader(ResourceUtils.getResourceStream(defaultRes), StandardCharsets.UTF_8);

        config = ConfigFactory.parseReader(reader);

        File customFile = FabricLoader.getInstance().getConfigDir().resolve("academy-craft-data.conf").toFile();
        if (!customFile.isFile()) {
            try {
                customFile.getParentFile().mkdirs();
                Files.copy(ResourceUtils.getResourceStream(defaultRes), customFile.toPath());
            } catch (IOException ex) {
                log.error("Error when copying config template to config folder", ex);
            }
        }

        try {
            Config customConfig = ConfigFactory.parseFile(customFile);

            config = customConfig.withFallback(config);
        } catch (RuntimeException ex) {
            log.error("An error occured parsing custom config", ex);
            lastError = ex.toString();
        }
    }

    public static void updateConfig(Config cfg) {
        if(cfg == null)
            __init();
        else
            config = cfg;
    }

    @StateEventCallback
    private static void __onInit(StateEvents.Init event) {
        instance();
        NetworkS11n.register(ACConfig.class);
        EventHub.BUS.register(new LoginEvents());
    }

    @StateEventCallback
    private static void __onServerStarting(StateEvents.ServerStarting event) {
        updateConfig(null);
    }

    public static Config instance() {
        synchronized (ACConfig.class) {
            if (config == null) {
                __init();
            }

            return config;
        }
    }

    /**
     * @return Last error, will clear the error storage. null if no error.
     */
    public static String fetchLastError() {
        String ret = lastError;
        lastError = null;
        return ret;
    }

    @Listener(channel = "sync_config", side = Side.CLIENT)
    private static void hSyncConfig(String rendered) {
        try {
            updateConfig(ConfigFactory.parseString(rendered));
        } catch (RuntimeException ex) {
            AcademyCraft.log.error("Failed to parse config sent from server", ex);
        }
    }

    public static class LoginEvents {
        @SubscribeEvent
        public void onPlayerLoggedIn(PlayerEvents.PlayerLoggedInEvent evt) {
            Player player = evt.player;
            String err = fetchLastError();

            if (err != null) {
                player.sendSystemMessage(Component.translatable("ac.data_config_parse_fail"));
                player.sendSystemMessage(Component.literal(err));
            }
            if (!player.level().isClientSide()) {
                NetworkMessage.sendTo(player, NetworkMessage.staticCaller(ACConfig.class), "sync_config",
                    instance().root().render());
            }
        }
    }

}
