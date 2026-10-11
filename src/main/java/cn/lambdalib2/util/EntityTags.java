package cn.lambdalib2.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Replacement of the 1.12 {@code Entity.getEntityData()}: an arbitrary (non persistent) tag attached to an entity.
 */
public final class EntityTags {

    private EntityTags() {}

    private static final Map<Entity, CompoundTag> tags = new WeakHashMap<>();

    public static synchronized CompoundTag get(Entity entity) {
        return tags.computeIfAbsent(entity, e -> new CompoundTag());
    }

}
