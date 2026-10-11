package cn.academy.advancements.triggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.Optional;

/**
 * A criterion trigger that is satisfied once the given ability level is reached.
 */
public class ACLevelTrigger extends SimpleCriterionTrigger<ACLevelTrigger.Instance> {

    private final int level;
    public final Identifier ID;

    public ACLevelTrigger(String domain, String name, int level) {
        this(Identifier.fromNamespaceAndPath(domain, name), level);
    }

    public ACLevelTrigger(String name, int level) {
        this(Identifier.fromNamespaceAndPath("academy", name), level);
    }

    public ACLevelTrigger(Identifier rl, int level) {
        ID = rl;
        this.level = level;
    }

    public Identifier getId() {
        return ID;
    }

    public int getLevel() {
        return level;
    }

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player, int currentLevel) {
        trigger(player, inst -> currentLevel >= inst.level().orElse(level));
    }

    public void trigger(ServerPlayer player) {
        trigger(player, level);
    }

    public record Instance(Optional<Holder<LootItemCondition>> player, Optional<Integer> level)
        implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
            LootItemCondition.CODEC.optionalFieldOf("player").forGetter(Instance::player),
            Codec.INT.optionalFieldOf("level").forGetter(Instance::level)
        ).apply(i, Instance::new));
    }

}
