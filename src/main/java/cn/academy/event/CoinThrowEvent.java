package cn.academy.event;
import cn.academy.entity.EntityCoinThrowing;
import net.minecraft.world.entity.player.Player;
import cn.lambdalib2.event.mc.PlayerEvents.PlayerEvent;


/**
 * @author WeAthFolD
 */
public class CoinThrowEvent extends PlayerEvent {
    
    public final EntityCoinThrowing coin;

    public CoinThrowEvent(Player _player, EntityCoinThrowing _coin) {
        super(_player);
        coin  = _coin;
    }

}