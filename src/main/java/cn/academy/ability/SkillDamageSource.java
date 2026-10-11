package cn.academy.ability;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Damage caused by a player's skill. Death message: "death.attack.ac_skill" with (target, player, skill name).
 */
public class SkillDamageSource extends DamageSource {

    public static final ResourceKey<DamageType> TYPE = ResourceKey.create(Registries.DAMAGE_TYPE,
        Identifier.fromNamespaceAndPath("academy", "ac_skill"));

    public static final ResourceKey<DamageType> TYPE_BYPASS_ARMOR = ResourceKey.create(Registries.DAMAGE_TYPE,
        Identifier.fromNamespaceAndPath("academy", "ac_skill_bypass_armor"));

    public final Skill skill;
    private final Player player;

    public static SkillDamageSource create(Player player, Skill skill, boolean bypassArmor) {
        Holder<DamageType> type = player.level().registryAccess()
            .lookupOrThrow(Registries.DAMAGE_TYPE)
            .getOrThrow(bypassArmor ? TYPE_BYPASS_ARMOR : TYPE);
        return new SkillDamageSource(type, player, skill);
    }

    private SkillDamageSource(Holder<DamageType> type, Player player, Skill skill) {
        super(type, player, player);
        this.skill = skill;
        this.player = player;
    }

    // Chat display
    @Override
    public Component getLocalizedDeathMessage(LivingEntity target) {
        return Component.translatable("death.attack.ac_skill",
                target.getDisplayName(),
                player.getDisplayName(),
                Component.translatable("ac.ability." + skill.getFullName() + ".name"));
    }

}
