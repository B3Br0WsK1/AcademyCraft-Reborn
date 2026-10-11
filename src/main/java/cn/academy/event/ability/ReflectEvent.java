package cn.academy.event.ability;
import cn.academy.ability.Skill;
import cn.lambdalib2.event.Cancelable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;


/**
 * If this event is canceled, a reflection to this attack event happens.
 */
@Cancelable
public class ReflectEvent extends AbilityEvent {

    public final Skill skill;
    public final Entity target;

    public ReflectEvent(Player player,
                        Skill        _skill,
                        Entity       _target) {
        super(player);

        skill = _skill;
        target = _target;
    }

}