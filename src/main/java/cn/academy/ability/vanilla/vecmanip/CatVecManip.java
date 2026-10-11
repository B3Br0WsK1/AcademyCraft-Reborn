package cn.academy.ability.vanilla.vecmanip;
import cn.academy.ability.Category;
import cn.academy.ability.vanilla.VanillaCategories;
import cn.academy.ability.vanilla.vecmanip.skill.*;


public class CatVecManip extends Category {

    public CatVecManip() {
        super("vecmanip");
        setColorStyle(0,0,0);

        DirectedShock.INSTANCE.setPosition(16, 45);
        Groundshock.INSTANCE.setPosition(64, 85);
        VecAccel.INSTANCE.setPosition(76, 40);
        VecDeviation.INSTANCE.setPosition(145, 53);
        DirectedBlastwave.INSTANCE.setPosition(136, 80);
        StormWing.INSTANCE.setPosition(130, 20);
        BloodRetrograde.INSTANCE.setPosition(204, 83);
        VecReflection.INSTANCE.setPosition(210, 50);
        PlasmaCannon.INSTANCE.setPosition(175, 14);

        // Level 1
        addSkill(DirectedShock.INSTANCE);
        addSkill(Groundshock.INSTANCE);

        // 2
        addSkill(VecAccel.INSTANCE);
        addSkill(VecDeviation.INSTANCE);

        // 3
        addSkill(DirectedBlastwave.INSTANCE);
        addSkill(StormWing.INSTANCE);

        // 4
        addSkill(BloodRetrograde.INSTANCE);
        addSkill(VecReflection.INSTANCE);

        // 5
        addSkill(PlasmaCannon.INSTANCE);

        Groundshock.INSTANCE.setParent(DirectedShock.INSTANCE);
        VecAccel.INSTANCE.setParent(DirectedShock.INSTANCE);
        VecDeviation.INSTANCE.setParent(VecAccel.INSTANCE);
        DirectedBlastwave.INSTANCE.setParent(Groundshock.INSTANCE);
        StormWing.INSTANCE.setParent(VecAccel.INSTANCE);
        BloodRetrograde.INSTANCE.setParent(DirectedBlastwave.INSTANCE);
        VecReflection.INSTANCE.setParent(VecDeviation.INSTANCE);
        PlasmaCannon.INSTANCE.setParent(StormWing.INSTANCE);

        VanillaCategories.addGenericSkills(this);
    }
}