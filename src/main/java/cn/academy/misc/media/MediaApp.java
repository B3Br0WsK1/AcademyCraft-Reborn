package cn.academy.misc.media;

import cn.academy.terminal.App;
import cn.academy.terminal.AppEnvironment;
import cn.academy.terminal.AppRegistry;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

/**
 * The media player terminal app.
 */
public class MediaApp extends App {

    public static final MediaApp INSTANCE = new MediaApp();

    private MediaApp() {
        super("media_player");
    }

    @StateEventCallback
    private static void init(StateEvents.PreInit ev) {
        AppRegistry.register(INSTANCE);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public AppEnvironment createEnvironment() {
        return new AppEnvironment() {
            @Override
            public void onStart() {
                Minecraft.getInstance().gui.setScreen(new MediaGui());
            }
        };
    }

}
