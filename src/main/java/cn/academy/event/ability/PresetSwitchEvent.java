package cn.academy.event.ability;
import net.minecraft.world.entity.player.Player;



/**
 * Fired only in client, when the player has just changed his preset using the
 * Preset Switch key(default 'C').
 * @author WeAthFolD
 */
public class PresetSwitchEvent extends AbilityEvent {

    public PresetSwitchEvent(Player _player) {
        super(_player);
    }

}