package cn.academy.terminal.app;
import cn.academy.ability.client.ui.SkillTreeAppUI;
import cn.academy.terminal.App;
import cn.academy.terminal.AppEnvironment;
import cn.academy.terminal.RegApp;
import net.minecraft.client.Minecraft;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;


/**
 * @author WeAthFolD
 */
public class AppSkillTree extends App {
    
    @RegApp
    public static AppSkillTree instance = new AppSkillTree();

    public AppSkillTree() {
        super("skill_tree");
    }

    @Override
    @Environment(EnvType.CLIENT)
    public AppEnvironment createEnvironment() {
        return new AppEnvironment() {
            @Environment(EnvType.CLIENT)
            @Override
            public void onStart() {
                Minecraft.getInstance().gui.setScreen(SkillTreeAppUI.apply());
            }
        };
    }

}