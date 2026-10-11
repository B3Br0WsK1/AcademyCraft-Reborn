/**
 * Copyright (c) Lambda Innovation, 2013-2016
 * This file is part of LambdaLib modding library.
 * https://github.com/LambdaInnovation/LambdaLib
 * Licensed under MIT, see project root for more information.
 */
package cn.lambdalib2.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Some commonly used entity selectors.
 * @author WeAthFolD
 */
public class EntitySelectors {

    public static Predicate<Entity> of(Class<? extends Entity> type) {
        return type::isInstance;
    }

    public static Predicate<Entity> everything() {
        return e -> true;
    }

    public static Predicate<Entity> nothing() {
        return e -> false;
    }

    public static Predicate<Entity> survivalPlayer() {
        return entity -> entity instanceof Player && !((Player) entity).getAbilities().instabuild;
    }

    public static Predicate<Entity> exclude(Entity... exclusions) {
        Set<Entity> set = new HashSet<>();
        Collections.addAll(set, exclusions);
        return entity -> !set.contains(entity);
    }

    public static Predicate<Entity> within(Entity entity, double range) {
        double sq = range * range;
        return e -> e.distanceToSqr(entity) <= sq;
    }

    public static Predicate<Entity> within(double x, double y, double z, double range) {
        double sq = range * range;
        return e -> e.distanceToSqr(x, y, z) <= sq;
    }

    public static Predicate<Entity> player() {
        return entity -> entity instanceof Player;
    }

    /**
     * @return The selector that returns living things. WARNING: Doesn't select only LivingEntity. Some special
     *  living types such as boss parts are taken into consideration.
     */
    public static Predicate<Entity> living() {
        return entity -> entity instanceof LivingEntity || entity instanceof EnderDragonPart;
    }

}
