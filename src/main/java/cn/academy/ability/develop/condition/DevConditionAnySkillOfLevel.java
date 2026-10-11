package cn.academy.ability.develop.condition;
import cn.academy.ability.Skill;
import cn.academy.datapart.AbilityData;
import cn.academy.ability.develop.IDeveloper;
import cn.academy.Resources;
import cn.academy.util.ACI18n;
import net.minecraft.resources.Identifier;


/**
 * @author WeAthFolD
 */
public class DevConditionAnySkillOfLevel implements IDevCondition {
    
    final int level;

    final Identifier res;

    public DevConditionAnySkillOfLevel(int _level) {
        level = _level;
        res = Resources.getTexture("abilities/condition/any" + level);
    }
    
    @Override
    public boolean accepts(AbilityData data, IDeveloper developer, Skill skill) {
        if(!data.hasCategory())
            return false;

        for(Skill s : data.getCategory().getSkillsOfLevel(level)) {
            if(data.isSkillLearned(s))
                return true;
        }
        return false;
    }

    @Override
    public Identifier getIcon() {
        return res;
    }

    @Override
    public String getHintText() {
        return ACI18n.translateFormatted("ac.skill_tree.anyskill", level);
    }

}