package cn.academy.ability.vanilla.vecmanip.skill;

import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.client.render.util.AnimPresets;
import cn.academy.client.render.util.IHandRenderer;
import cn.academy.client.render.util.VanillaHandRenderer;
import cn.academy.client.sound.ACSounds;
import cn.academy.datapart.HandRenderOverrideData;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.EntitySelectors;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.RayTraceResult;
import cn.lambdalib2.util.Raytrace;
import cn.lambdalib2.util.Side;
import cn.lambdalib2.vis.animation.presets.CompTransformAnim;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.function.DoubleSupplier;

import static cn.lambdalib2.util.MathUtils.lerpf;
import static cn.lambdalib2.util.VecUtils.entityHeadPos;
import static cn.lambdalib2.util.VecUtils.multiply;
import static cn.lambdalib2.util.VecUtils.subtract;

/**
 * Directed shock. Ported from Scala.
 */
public class DirectedShock extends Skill {

    public static final DirectedShock INSTANCE = new DirectedShock();

    private DirectedShock() {
        super("dir_shock", 1);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        activateSingleKey(rt, keyID, ShockContext::new);
    }

    public static final String MSG_PERFORM = "perform";
    public static final String MSG_GENERATE_EFFECT = "gen_eff";

    public static class ShockContext extends Context<DirectedShock> {

        private static final int MIN_TICKS = 6;
        private static final int MAX_ACCEPTED_TICKS = 50;
        private static final int MAX_TOLERANT_TICKS = 200;
        private static final int PUNCH_ANIM_TICKS = 6;

        private int ticker = 0;

        private boolean punched = false;
        private int punchTicker = 0;

        private final float damage = lerpf(7, 15, ctx.getSkillExp());

        public ShockContext(Player p) {
            super(p, INSTANCE);
        }

        @Listener(channel=MSG_KEYUP, side=Side.CLIENT)
        public void l_keyUp() {
            if (ticker > MIN_TICKS && ticker < MAX_ACCEPTED_TICKS) {
                sendToServer(MSG_PERFORM, ticker);
            } else {
                terminate();
            }
        }

        @Listener(channel=MSG_KEYABORT, side=Side.CLIENT)
        public void l_keyAbort() {
            terminate();
        }

        @Listener(channel=MSG_TICK, side=Side.CLIENT)
        public void l_tick() {
            if (!isLocal()) return;
            ticker += 1;
            if (ticker >= MAX_TOLERANT_TICKS) {
                terminate();
            }
            if (punched) {
                punchTicker += 1;
            }
            if (punched && punchTicker > PUNCH_ANIM_TICKS) {
                terminate();
            }
        }

        @Listener(channel=MSG_PERFORM, side=Side.SERVER)
        public void s_perform(int ticks) {
            sendToClient(MSG_PERFORM, ticks);

            if (consume()) {
                RayTraceResult trace = Raytrace.traceLiving(player, 3, EntitySelectors.living());
                if (trace != null && trace.typeOfHit == RayTraceResult.Type.ENTITY) {
                    Entity entity = trace.entityHit;
                    ctx.attack(entity, damage);
                    knockback(entity);
                    ctx.setCooldown((int) lerpf(60, 20, ctx.getSkillExp()));
                    sendToClient(MSG_GENERATE_EFFECT, entity);

                    Vec3 delta = multiply(subtract(entity.position(), player.position()).normalize(), 0.24);
                    entity.setDeltaMovement(entity.getDeltaMovement().add(delta));
                    entity.needsSync = true;

                    ctx.addSkillExp(0.0035f);
                } else {
                    ctx.addSkillExp(0.0010f);
                }
            }

            terminate();
        }

        @Listener(channel=MSG_GENERATE_EFFECT, side=Side.CLIENT)
        public void c_effect(Entity ent) {
            if (ent != null) knockback(ent);
            punched = true;
        }

        private boolean consume() {
            float cp = lerpf(50, 100, ctx.getSkillExp());
            float overload = lerpf(18, 12, ctx.getSkillExp());

            return ctx.consume(overload, cp);
        }

        private void knockback(Entity targ) {
            if (ctx.getSkillExp() >= 0.25f) {
                Vec3 delta = subtract(entityHeadPos(player), entityHeadPos(targ));
                delta = delta.normalize();
                delta = new Vec3(delta.x, delta.y - 0.6f, delta.z).normalize();

                targ.setPos(targ.getX(), targ.getY() + 0.1, targ.getZ());
                targ.setDeltaMovement(delta.x * -0.7f, delta.y * -0.7f, delta.y * -0.7f);
                targ.needsSync = true;
            }
        }

    }

    @Environment(EnvType.CLIENT)
    @RegClientContext(ShockContext.class)
    public static class ShockContextC extends ClientContext {

        IHandRenderer handEffect;

        CompTransformAnim anim;

        DoubleSupplier timeProvider = null;

        public ShockContextC(Context par) {
            super(par);
        }

        @Listener(channel=MSG_GENERATE_EFFECT, side=Side.CLIENT)
        public void l_effect() {
            if (!isLocal()) return;
            double init = GameTimer.getTime();
            timeProvider = () -> (GameTimer.getTime() - init) / 0.3;

            anim = AnimPresets.createPunchAnim();
            anim.perform(0);
        }

        @Listener(channel=MSG_GENERATE_EFFECT, side=Side.CLIENT)
        public void c_effect() {
            ACSounds.playClient(player, "vecmanip.directed_shock", SoundSource.AMBIENT, 0.5f);
        }

        @Listener(channel=MSG_MADEALIVE, side=Side.CLIENT)
        public void l_handEffectStart() {
            if (!isLocal()) return;
            anim = AnimPresets.createPrepareAnim();

            double init = GameTimer.getTime();
            timeProvider = () -> Math.min(2.0, (GameTimer.getTime() - init) / 0.15);

            handEffect = partialTicks -> {
                anim.perform(timeProvider.getAsDouble());
                VanillaHandRenderer.renderHand(partialTicks, anim.target);
            };

            HandRenderOverrideData.get(player).addInterrupt(handEffect);
        }

        @Listener(channel=MSG_TERMINATED, side=Side.CLIENT)
        public void l_handEffectTerminate() {
            if (isLocal()) {
                HandRenderOverrideData.get(player).stopInterrupt(handEffect);
            }
        }

    }

}
