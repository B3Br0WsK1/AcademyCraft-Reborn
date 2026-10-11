package cn.academy.event;
import cn.lambdalib2.event.Event;
import net.minecraft.world.entity.player.Player;


/**
 * Fired both at client and server when player installed data terminal.
 * @author WeAthFolD
 */
public class TerminalInstalledEvent extends Event {
    
    final Player player;
    
    public TerminalInstalledEvent(Player _player) {
        player = _player;
    }
    
}