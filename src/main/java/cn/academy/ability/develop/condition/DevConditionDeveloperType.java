package cn.academy.ability.develop.condition;
import cn.academy.ability.Skill;
import cn.academy.datapart.AbilityData;
import cn.academy.ability.AbilityLocalization;
import cn.academy.ability.develop.DeveloperType;
import cn.academy.ability.develop.IDeveloper;
import net.minecraft.resources.Identifier;


/**
 * @author WeAthFolD
 */
public class DevConditionDeveloperType implements IDevCondition {
    
    final DeveloperType type;
    
    public DevConditionDeveloperType(DeveloperType _type) {
        type = _type;
    }

    @Override
    public boolean accepts(AbilityData data, IDeveloper developer, Skill skill) {
        return developer.getDeveloperType().ordinal() >= type.ordinal();
    }
    
    @Override
    public Identifier getIcon() {
        return type.texture;
    }

    @Override
    public String getHintText() {
        return AbilityLocalization.instance.machineType(type);
    }

}