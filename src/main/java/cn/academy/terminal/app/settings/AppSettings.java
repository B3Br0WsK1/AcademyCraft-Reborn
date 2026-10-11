package cn.academy.terminal.app.settings;
import cn.academy.terminal.App;
import cn.academy.terminal.AppEnvironment;
import cn.academy.terminal.RegApp;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.fabricmc.api.EnvType;


/**
 * @author WeAthFolD
 *
 */
public class AppSettings extends App {

    @RegApp(priority = -1)
    public static AppSettings instance = new AppSettings();
    
    private AppSettings() {
        super("settings");
        setPreInstalled();
    }

    @Override
    @Environment(EnvType.CLIENT)
    public AppEnvironment createEnvironment() {
        return new AppEnvironment() {
            @Override
            @Environment(EnvType.CLIENT)
            public void onStart() {
                Minecraft.getInstance().gui.setScreen(new SettingsUI());
            }
        };
    }
}