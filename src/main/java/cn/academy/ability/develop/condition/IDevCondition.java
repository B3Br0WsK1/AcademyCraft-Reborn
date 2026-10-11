package cn.academy.ability.develop.condition;
import cn.academy.ability.Skill;
import cn.academy.datapart.AbilityData;
import cn.academy.ability.develop.IDeveloper;
import net.minecraft.resources.Identifier;


/**
 * Skill development constraint. This should represent a single atomic constraint on player.
 * If it has a icon to display, it will be displayed on the developer's skill description page.
 * @author WeAthFolD
 */
public interface IDevCondition {
    
    /**
     * @param developer The develope that performs this action
     * @return Whether the player can learn the given skill
     */
    boolean accepts(AbilityData data, IDeveloper developer, Skill skill);
    
    /**
     * @return The icon displayed in skill desc page
     */
    Identifier getIcon();
    
    /**
     * @return The hovering text displayed in skill desc page
     */
    String getHintText();

    default boolean shouldDisplay() {
        return true;
    }
    
}