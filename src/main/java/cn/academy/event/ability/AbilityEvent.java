package cn.academy.event.ability;
import cn.academy.datapart.AbilityData;
import cn.lambdalib2.event.Event;
import net.minecraft.world.entity.player.Player;


/**
 * @author WeAthFolD
 */
public abstract class AbilityEvent extends Event {
    
    public final Player player;
    
    public AbilityEvent(Player _player) {
        player = _player;
    }
    
    public AbilityData getAbilityData() {
        return AbilityData.get(player);
    }
    
}