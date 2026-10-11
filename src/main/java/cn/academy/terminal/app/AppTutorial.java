package cn.academy.terminal.app;
import cn.academy.Resources;
import cn.academy.client.gui.GuiTutorial;
import cn.academy.terminal.App;
import cn.academy.terminal.AppEnvironment;
import cn.academy.terminal.RegApp;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;


public class AppTutorial extends App {
    @RegApp
    private static final AppTutorial instance = new AppTutorial();

    public AppTutorial() {
        super("tutorial");
        setPreInstalled();
    }

    @Override
    @Environment(EnvType.CLIENT)
    public AppEnvironment createEnvironment() {
        return new AppEnvironment() {
            @Override
            @Environment(EnvType.CLIENT)
            public void onStart() {
                Minecraft.getInstance().gui.setScreen(new GuiTutorial());
            }
        };
    }

    // Random gives icon for more fun >)
    @Override
    @Environment(EnvType.CLIENT)
    public Identifier getIcon() {
        float rand = RandUtils.nextFloat();
        if (rand < 0.2f) {
            return icon(0);
        } else if (rand < 0.3f) {
            return icon(1);
        } else {
            return icon(2);
        }
    }

    @Environment(EnvType.CLIENT)
    private Identifier icon(int id) {
        return Resources.preloadMipmapTexture("guis/apps/tutorial/icon_" + id);
    }
}
