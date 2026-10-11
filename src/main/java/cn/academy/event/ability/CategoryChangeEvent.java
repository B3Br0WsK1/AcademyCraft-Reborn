package cn.academy.event.ability;
import net.minecraft.world.entity.player.Player;


/**
 * Fired in both sides, when the player category has just changed.
 * @author WeAthFolD
 */
public class CategoryChangeEvent extends AbilityEvent {

    public CategoryChangeEvent(Player _player) {
        super(_player);
    }
    
}