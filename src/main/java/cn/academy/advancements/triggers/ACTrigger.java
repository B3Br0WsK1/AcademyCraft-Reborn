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
 * A simple criterion trigger that fires whenever {@link #trigger(ServerPlayer)} is called.
 */
public class ACTrigger extends SimpleCriterionTrigger<ACTrigger.Instance> {

    public final Identifier ID;

    public ACTrigger(String domain, String name) {
        this(Identifier.fromNamespaceAndPath(domain, name));
    }

    public ACTrigger(String name) {
        this(Identifier.fromNamespaceAndPath("academy", name));
    }

    public ACTrigger(Identifier rl) {
        ID = rl;
    }

    public Identifier getId() {
        return ID;
    }

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player) {
        trigger(player, inst -> true);
    }

    public record Instance(Optional<Holder<LootItemCondition>> player) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
            LootItemCondition.CODEC.optionalFieldOf("player").forGetter(Instance::player)
        ).apply(i, Instance::new));
    }

}
