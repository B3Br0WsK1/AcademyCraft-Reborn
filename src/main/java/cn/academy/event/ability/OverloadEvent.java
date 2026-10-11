package cn.academy.event.ability;
import net.minecraft.world.entity.player.Player;


/**
 * Fired when player is overloaded.
 * Side: Server only
 */
public class OverloadEvent extends AbilityEvent {
    public OverloadEvent(Player _player) {
        super(_player);
    }
}