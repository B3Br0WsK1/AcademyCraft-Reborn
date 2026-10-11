package cn.academy.ability.vanilla.electromaster.skill;

import cn.academy.ability.AbilityContext;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;

import java.lang.reflect.Field;

public class EMDamageHelper {

    private static EntityDataAccessor<Boolean> CREEPER_POWERED = null;

    /**
     * Attack with a chance to generate a high-voltage creeper.
     */
    @SuppressWarnings("unchecked")
    public static void attack(AbilityContext ctx, Entity target, float dmg) {
        ctx.attack(target, dmg);
        if(target instanceof Creeper creeper) {
            if(RandUtils.nextFloat() < 0.3f) {
                if(CREEPER_POWERED == null) {
                    try {
                        Field field = Creeper.class.getDeclaredField("DATA_IS_POWERED");
                        field.setAccessible(true);
                        CREEPER_POWERED = (EntityDataAccessor<Boolean>) field.get(null);
                    } catch (ReflectiveOperationException e) {
                        e.printStackTrace();
                        return;
                    }
                }
                // Set the creeper to be powered
                creeper.getEntityData().set(CREEPER_POWERED, true);
            }
        }
    }

}
