package cn.academy.event.ability;
import net.minecraft.world.entity.player.Player;


/**
 * Fired at server when player's ability level has changed.
 * @author WeAthFolD
 */
public class LevelChangeEvent extends AbilityEvent {
    
    public LevelChangeEvent(Player p) {
        super(p);
    }
    
}