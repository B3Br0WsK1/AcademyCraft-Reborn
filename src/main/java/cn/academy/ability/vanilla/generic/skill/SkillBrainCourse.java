package cn.academy.ability.vanilla.generic.skill;
import cn.academy.ability.Skill;
import cn.academy.datapart.AbilityData;
import cn.academy.event.ability.CalcEvent;
import cn.lambdalib2.event.EventHub;
import cn.lambdalib2.event.SubscribeEvent;


/**
 * Generic skill: Brain Course.
 * @author WeAthFolD
 */
public class SkillBrainCourse extends Skill {

    public SkillBrainCourse() {
        super("brain_course", 3);
        this.canControl = false;
        this.isGeneric = true;
        EventHub.BUS.register(this);
    }

    @SubscribeEvent
    public void recalcMaxCP(CalcEvent.MaxCP event) {
        AbilityData adata = AbilityData.get(event.player);
        if (adata.isSkillLearned(this)) {
            event.value += 1000;
        }
    }

}