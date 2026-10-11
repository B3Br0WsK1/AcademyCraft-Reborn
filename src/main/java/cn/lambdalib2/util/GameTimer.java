package cn.lambdalib2.util;

import cn.lambdalib2.event.SubscribeEvent;
import cn.lambdalib2.event.mc.TickEvent;
import cn.lambdalib2.registry.mc.RegEventHandler;
import net.minecraft.util.Util;

/**
 * A simple timer wrapup to handle paused timing situations.
 * @author WeAthFolD
 */
public enum GameTimer {
    @RegEventHandler
    INSTANCE;

    /**
     * Client hook used to know whether the game is paused. Installed by client init.
     */
    public interface PauseHook {
        boolean isPaused();
    }

    private static PauseHook pauseHook = () -> false;

    static long storedTime, timeLag;

    static long beginTimeClient, beginTimeServer;

    public static void _setPauseHook(PauseHook hook) {
        pauseHook = hook;
    }

    public static double getTime() {
        boolean isClient = SideUtils.getRuntimeSide() == Side.CLIENT;
        return getTime(isClient);
    }

    public static double getAbsTime() {
        return getTime(false);
    }

    private static double getTime(boolean isClient) {
        if (isClient) {
            if (beginTimeClient == 0) beginTimeClient = getRawTimeClient();
            long elapsed = getRawTimeClient() - beginTimeClient;
            return elapsed / 1000.0;
        } else {
            if (beginTimeServer == 0) beginTimeServer = getRawTimeServer();
            long elapsed = getRawTimeServer() - beginTimeServer;
            return elapsed / 1000.0;
        }
    }

    private static synchronized long getRawTimeClient() {
        long time = Util.getMillis();
        if (pauseHook.isPaused()) {
            timeLag = time - storedTime;
        } else {
            storedTime = time - timeLag;
        }
        return time - timeLag;
    }

    private static long getRawTimeServer() {
        return Util.getMillis();
    }

    // In case GameTimer isn't queried frequently, use this to prevent huge (and incorrect) time lag.
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        getRawTimeClient();
    }

}
