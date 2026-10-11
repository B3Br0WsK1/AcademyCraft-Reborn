package cn.academy.event.ability;
import net.minecraft.world.entity.player.Player;


/**
 * Fired both CLIENT and SERVER, when player has deactivated
 * his/her ability using activate key ('V').
 * @author WeAthFolD
 */
public class AbilityDeactivateEvent extends AbilityEvent {

    public AbilityDeactivateEvent(Player _player) {
        super(_player);
    }

}