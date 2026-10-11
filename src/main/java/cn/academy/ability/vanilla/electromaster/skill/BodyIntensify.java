package cn.academy.ability.vanilla.electromaster.skill;

import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.client.auxgui.CurrentChargingHUD;
import cn.academy.client.sound.ACSounds;
import cn.academy.client.sound.FollowEntitySound;
import cn.academy.entity.EntityIntensifyEffect;
import cn.lambdalib2.auxgui.AuxGuiHandler;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static cn.lambdalib2.util.MathUtils.lerp;
import static cn.lambdalib2.util.MathUtils.lerpf;

/**
 * Body Intensify. Ported from Scala.
 * @author WeAthFolD, KSkun
 */
public class BodyIntensify extends Skill {

    public static final BodyIntensify INSTANCE = new BodyIntensify();

    public static final int MIN_TIME = 10;
    public static final int MAX_TIME = 40;
    public static final int MAX_TOLERANT_TIME = 100;
    public static final String LOOP_SOUND = "em.intensify_loop";
    public static final String ACTIVATE_SOUND = "em.intensify_activate";

    private record EffectType(Holder<MobEffect> effect, int maxAmplifier) {}

    private static final List<EffectType> effects = List.of(
        new EffectType(MobEffects.SPEED, 3),
        new EffectType(MobEffects.JUMP_BOOST, 1),
        new EffectType(MobEffects.REGENERATION, 1),
        new EffectType(MobEffects.STRENGTH, 1),
        new EffectType(MobEffects.RESISTANCE, 1)
    );

    private BodyIntensify() {
        super("body_intensify", 3);
    }

    private static MobEffectInstance createEffect(EffectType effect, int level, int duration) {
        return new MobEffectInstance(effect.effect(), duration, Math.min(level, effect.maxAmplifier()), false, true);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void activate(ClientRuntime rt, int keyid) {
        activateSingleKey(rt, keyid, IntensifyContext::new);
    }

    public static final String MSG_EFFECT_END = "effect_end";
    public static final String MSG_END = "end";

    public static class IntensifyContext extends Context<BodyIntensify> {

        private int tick = 0;
        private final float consumption = lerpf(20, 15, ctx.getSkillExp());

        private float overload = 0f;

        public IntensifyContext(Player p) {
            super(p, INSTANCE);
        }

        private double getProbability(int ct) {
            return (ct - 10.0) / 18.0;
        }

        private int getBuffTime(int ct) {
            return (int) (RandUtils.ranged(1, 2) * ct * lerp(1.5, 2.5, ctx.getSkillExp()));
        }

        private int getHungerBuffTime(int ct) {
            return (int) (1.25f * ct);
        }

        private int getBuffLevel(int ct) {
            return (int) Math.floor(getProbability(ct));
        }

        @Listener(channel=MSG_MADEALIVE, side=Side.SERVER)
        private void s_consume() {
            float overload = lerpf(200, 120, ctx.getSkillExp());
            ctx.consume(overload, 0);
            this.overload = ctx.cpData.getOverload();
        }

        @Listener(channel=MSG_TICK, side=Side.SERVER)
        private void s_onTick() {
            if (ctx.cpData.getOverload() < overload) ctx.cpData.setOverload(overload);
            tick += 1;
            if ((tick <= MAX_TIME && !ctx.consume(0, consumption)) || tick >= MAX_TOLERANT_TIME) {
                sendToClient(MSG_EFFECT_END, false);
                terminate();
            }
        }

        @Listener(channel=MSG_END, side=Side.SERVER)
        private void s_onEnd() {
            if (tick >= MIN_TIME) {
                if (tick >= MAX_TIME) tick = MAX_TIME;
                List<EffectType> shuffled = new ArrayList<>(effects);
                Collections.shuffle(shuffled);
                double p = getProbability(tick);
                int i = 0;
                int time = getBuffTime(tick);

                while (p > 0) {
                    double a = RandUtils.ranged(0, 1);
                    if (a < p && i < shuffled.size()) {
                        // Spawn a new buff
                        int level = getBuffLevel(tick);
                        player.addEffect(createEffect(shuffled.get(i), level, time));
                        i += 1;
                    }

                    p -= 1.0;
                }

                // Also give him a hunger buff
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, getHungerBuffTime(tick), 2));
                ctx.addSkillExp(0.01f);

                int cooldown = (int) lerpf(900, 600, ctx.getSkillExp());
                ctx.setCooldown(cooldown);
                sendToClient(MSG_EFFECT_END, true);
                terminate();
            } else {
                sendToClient(MSG_EFFECT_END, false);
                terminate();
            }
        }

        @Listener(channel=MSG_KEYUP, side=Side.CLIENT)
        private void l_onEnd() {
            sendToServer(MSG_END);
        }

        @Listener(channel=MSG_KEYABORT, side=Side.CLIENT)
        private void l_onAbort() {
            sendToSelf(MSG_EFFECT_END, false);
            terminate();
        }

    }

    @Environment(EnvType.CLIENT)
    @RegClientContext(IntensifyContext.class)
    public static class IntensifyContextC extends ClientContext {

        FollowEntitySound loopSound;

        CurrentChargingHUD hud;

        public IntensifyContextC(Context par) {
            super(par);
        }

        @Listener(channel=MSG_MADEALIVE, side=Side.CLIENT)
        private void c_startEffect() {
            if (isLocal()) {
                loopSound = new FollowEntitySound(player, LOOP_SOUND, SoundSource.AMBIENT).setLoop();
                hud = new CurrentChargingHUD();
                ACSounds.playClient(loopSound);
                AuxGuiHandler.register(hud);
            }
        }

        @Listener(channel=MSG_EFFECT_END, side=Side.CLIENT)
        private void c_endEffect(boolean performed) {
            if (isLocal()) {
                if (loopSound != null) loopSound.stop();
                if (hud != null) hud.startBlend(performed);
            }

            if (performed) {
                ACSounds.playClient(player, ACTIVATE_SOUND, SoundSource.AMBIENT, 0.5f);
                player.level().addFreshEntity(new EntityIntensifyEffect(player));
            }
        }

        @Listener(channel=MSG_TERMINATED, side=Side.CLIENT)
        private void c_terminated() {
            if (loopSound != null) loopSound.stop();
            if (hud != null) hud.startBlend(false);
        }
    }

}
