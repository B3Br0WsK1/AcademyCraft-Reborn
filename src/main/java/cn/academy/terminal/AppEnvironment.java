package cn.academy.terminal;
import cn.academy.client.auxgui.TerminalUI;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.fabricmc.api.EnvType;


/**
 * @author WeAthFolD
 */
@Environment(EnvType.CLIENT)
public class AppEnvironment {

    /*
     * Instances to be injected when startup
     */
    public App app;
    public TerminalUI terminal;

    /**
     * Called just before environment is activated on client side. Load the
     * data.
     */
    public void onStart() {
    }

    protected App getApp() {
        return app;
    }

    protected TerminalUI getTerminal() {
        return terminal;
    }

    protected Player getPlayer() {
        return Minecraft.getInstance().player;
    }

}