package cn.academy.event.ability;
import cn.academy.ability.Skill;
import net.minecraft.world.entity.player.Player;


/**
 * Fired in BOTH CLIENT AND SERVER, when an Skill is newly learned by the player.
 * @author WeAthFolD
 */
public class SkillLearnEvent extends AbilityEvent {
    
    public final Skill skill;

    public SkillLearnEvent(Player player, Skill _skill) {
        super(player);
        skill = _skill;
    }

}