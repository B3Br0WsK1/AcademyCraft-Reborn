package cn.academy.ability.vanilla.vecmanip.skill;

import cn.academy.ACConfig;
import cn.lambdalib2.util.EntityTags;
import com.typesafe.config.Config;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Handles entity affection of VecDeviation and VecReflection. Ported from Scala.
 * Entity names are now entity type ids (e.g. "minecraft:zombie").
 */
public final class EntityAffection {

    private EntityAffection() {}

    public sealed interface AffectInfo permits Excluded, Affected {}
    public record Excluded() implements AffectInfo {}
    public record Affected(float difficulty) implements AffectInfo {}

    private record DifficultyEntry(EntityType<?> type, float difficulty) {}

    private static List<DifficultyEntry> entityData;
    private static List<Predicate<Entity>> excluded;

    private static Optional<EntityType<?>> typeOf(String name) {
        Identifier id = Identifier.tryParse(name);
        if (id == null) return Optional.empty();
        return BuiltInRegistries.ENTITY_TYPE.getOptional(id);
    }

    private static void load() {
        if (entityData != null) return;
        Config cfg = ACConfig.instance().getConfig("ac.ability.category.vecmanip.common.affected_entities");

        List<DifficultyEntry> data = new ArrayList<>();
        for (Config elem : cfg.getConfigList("difficulties")) {
            float difficulty = (float) elem.getDouble("difficulty");
            typeOf(elem.getString("name")).ifPresent(t -> data.add(new DifficultyEntry(t, difficulty)));
        }

        List<Predicate<Entity>> ex = new ArrayList<>();
        for (String key : cfg.getStringList("excluded")) {
            if (key.equalsIgnoreCase("living")) {
                ex.add(e -> e instanceof LivingEntity);
            } else if (key.equalsIgnoreCase("mob")) {
                ex.add(e -> e instanceof Monster);
            } else {
                typeOf(key).ifPresent(t -> ex.add(e -> e.getType() == t));
            }
        }

        entityData = data;
        excluded = ex;
    }

    public static AffectInfo getAffectInfo(Entity entity) {
        load();
        for (Predicate<Entity> p : excluded) {
            if (p.test(entity)) return new Excluded();
        }
        for (DifficultyEntry entry : entityData) {
            if (entry.type() == entity.getType()) return new Affected(entry.difficulty());
        }
        return new Affected(1);
    }

    public static void mark(Entity targ) {
        EntityTags.get(targ).putBoolean("ac_vm_deviated", true);
    }

    public static boolean isMarked(Entity targ) {
        return EntityTags.get(targ).getBooleanOr("ac_vm_deviated", false);
    }

}
