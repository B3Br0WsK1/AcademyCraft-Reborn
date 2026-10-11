package cn.academy.ability.vanilla.vecmanip.skill;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;

import java.lang.reflect.Method;

/**
 * Ported from Scala.
 */
public final class VMSkillHelper {

    private VMSkillHelper() {}

    private static Method inGroundMethod;

    public static boolean isInGround(AbstractArrow arrow) {
        try {
            if (inGroundMethod == null) {
                inGroundMethod = AbstractArrow.class.getDeclaredMethod("isInGround");
                inGroundMethod.setAccessible(true);
            }
            return (boolean) inGroundMethod.invoke(arrow);
        } catch (ReflectiveOperationException e) {
            return arrow.getDeltaMovement().lengthSqr() < 1e-6;
        }
    }

}
