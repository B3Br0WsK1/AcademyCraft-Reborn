package cn.academy;

import cn.lambdalib2.LambdaLib2;
import cn.lambdalib2.LambdaLib2Client;
import cn.lambdalib2.util.Configuration;
import cn.lambdalib2.util.SideUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Academy Craft Mod Main Class
 *
 * @author acaly, WeathFolD, KS
 */
public class AcademyCraft implements ModInitializer {

    public static final String MODID = "academy";

    public static final String VERSION = FabricLoader.getInstance().getModContainer(MODID)
            .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("dev");

    public static final boolean DEBUG_MODE = FabricLoader.getInstance().isDevelopmentEnvironment();

    public static final Logger log = LoggerFactory.getLogger("AcademyCraft");

    /**
     * User-side settings (keys, GUI options, generic switches), stored in config/academy.cfg.
     */
    public static Configuration config;

    @Override
    public void onInitialize() {
        log.info("Starting AcademyCraft");
        log.info("Copyright (c) Lambda Innovation, 2013-2018");
        log.info("https://ac.li-dev.cn/");

        config = new Configuration(FabricLoader.getInstance().getConfigDir().resolve("academy.cfg").toFile());

        if (SideUtils.isPhysicalClient()) {
            LambdaLib2Client.installHooks();
        }

        LambdaLib2.init(MODID);
        LambdaLib2.runCommonStages();

        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(server -> config.save());
        config.save();
    }

    /**
     * Simply a fast route to print debug message.
     */
    public static void debug(Object msg) {
        if (DEBUG_MODE) {
            log.info(String.valueOf(msg));
        }
    }

}
