package cn.academy.event;
import cn.academy.terminal.App;
import cn.lambdalib2.event.Event;
import net.minecraft.world.entity.player.Player;


/**
 * Fired in both client and server when any app is installed, disregarding pre-installed apps.
 * @author WeAthFolD
 */
public class AppInstalledEvent extends Event {
    
    public final Player player;
    public final App app;
    
    public AppInstalledEvent(Player _player, App _app) {
        player = _player;
        app = _app;
    }
    
}