package cn.academy.ability.vanilla.meltdowner.skill;

import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.client.render.particle.MdParticleFactory;
import cn.academy.entity.EntityDiamondShield;
import cn.academy.entity.EntityRippleMark;
import cn.lambdalib2.particle.Particle;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.EntitySelectors;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.RayTraceResult;
import cn.lambdalib2.util.Raytrace;
import cn.lambdalib2.util.Side;
import cn.lambdalib2.util.VecUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import static cn.lambdalib2.util.MathUtils.lerpf;

/**
 * Jet engine. Ported from Scala.
 * @author WeAthFolD, KSkun
 */
public class JetEngine extends Skill {

    public static final JetEngine INSTANCE = new JetEngine();

    private JetEngine() {
        super("jet_engine", 4);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        activateSingleKey(rt, keyID, JEContext::new);
    }

    public static final String MSG_TRIGGER = "trigger";
    public static final String MSG_MARK_END = "mark_end";

    static Vec3 getDest(Player player) {
        return Raytrace.getLookingPos(player, 12, EntitySelectors.nothing()).getLeft();
    }

    public static class JEContext extends Context<JetEngine> {

        private final float exp = ctx.getSkillExp();
        private final float consumption = lerpf(170, 140, exp);
        private final float overload = lerpf(60, 50, exp);

        // TRIGGER
        private static final float TIME = 8;
        private static final float LIFETIME = 15;
        private Vec3 target;
        private int ticks = 0;
        private boolean isTriggering = false;

        private Vec3 start;
        private Vec3 velocity;

        public JEContext(Player p) {
            super(p, INSTANCE);
        }

        @Listener(channel=MSG_TICK, side=Side.SERVER)
        private void s_onTick() {
            if (!ctx.canConsumeCP(consumption)) terminate();
        }

        @Listener(channel=MSG_KEYUP, side=Side.CLIENT)
        private void l_onKeyUp() {
            sendToServer(MSG_MARK_END);
        }

        @Listener(channel=MSG_KEYABORT, side=Side.CLIENT)
        private void l_onKeyAbort() {
            sendToSelf(MSG_MARK_END);
            terminate();
        }

        @Listener(channel=MSG_MARK_END, side=Side.SERVER)
        private void s_onEnd() {
            if (ctx.consume(consumption, overload)) {
                sendToClient(MSG_MARK_END);
                sendToSelf(MSG_TRIGGER, getDest(player));
                ctx.addSkillExp(.004f);
                ctx.setCooldown((int) lerpf(60, 30, exp));
            } else {
                sendToClient(MSG_MARK_END);
                terminate();
            }
        }

        @Listener(channel=MSG_TRIGGER, side=Side.SERVER)
        private void s_triggerStart(Vec3 _target) {
            target = _target;
            isTriggering = true;

            sendToClient(MSG_TRIGGER, target);
        }

        @Listener(channel=MSG_TRIGGER, side=Side.CLIENT)
        private void c_triggerStart(Vec3 _target) {
            if (isLocal()) {
                isTriggering = true;
                target = _target;

                start = player.position();
                velocity = VecUtils.multiply(VecUtils.subtract(target, start), 1.0 / TIME);
            }
        }

        @Listener(channel=MSG_TICK, side=Side.SERVER)
        private void s_triggerTick() {
            if (isTriggering) {
                RayTraceResult pos = Raytrace.perform(world(),
                    new Vec3(player.xOld, player.yOld, player.zOld),
                    player.position(), EntitySelectors.exclude(player).and(EntitySelectors.living()));
                if (player.getVehicle() != null) player.stopRiding();
                if (pos != null && pos.entityHit != null) MDDamageHelper.attack(ctx, pos.entityHit, lerpf(7, 20, exp));
            }
        }

        @Listener(channel=MSG_TICK, side=Side.CLIENT)
        private void c_triggerTick() {
            if (isLocal() && isTriggering) {
                if (ticks >= LIFETIME)
                    terminate();
                ticks += 1;
                Vec3 pos = VecUtils.lerp(start, target, ticks / TIME);
                player.setPos(pos.x, pos.y, pos.z);
                player.setDeltaMovement(velocity);
                player.resetFallDistance();
            }
        }

    }

    @Environment(EnvType.CLIENT)
    @RegClientContext(JEContext.class)
    public static class JEContextC extends ClientContext {

        private static final float TIME = 8;

        private EntityRippleMark mark;

        private Vec3 target;
        private final Vec3 start;

        private boolean isMarking = false;
        private int ticks = 0;

        // TRIGGER
        private EntityDiamondShield entity;
        private boolean isTriggering = false;

        public JEContextC(Context par) {
            super(par);
            start = player.position();
        }

        @Listener(channel=MSG_MADEALIVE, side=Side.CLIENT)
        private void l_spawnMark() {
            if (isLocal()) {
                isMarking = true;
                mark = new EntityRippleMark(player.level());
                player.level().addFreshEntity(mark);
                mark.color.set(51, 255, 51, 179);
            }
        }

        @Listener(channel=MSG_TICK, side=Side.CLIENT)
        private void l_updateMark() {
            if (isLocal() && isMarking && mark != null) {
                Vec3 dest = getDest(player);
                mark.setPos(dest.x, dest.y, dest.z);
            }
        }

        @Listener(channel=MSG_MARK_END, side=Side.CLIENT)
        private void l_endMark() {
            if (isLocal()) {
                isMarking = false;
                if (mark != null) mark.setDead();
            }
        }

        @Listener(channel=MSG_TRIGGER, side=Side.CLIENT)
        private void c_tStartEffect(Vec3 _target) {
            target = _target;
            isTriggering = true;
            entity = new EntityDiamondShield(player);
            player.level().addFreshEntity(entity);
        }

        @Listener(channel=MSG_TICK, side=Side.CLIENT)
        private void c_tUpdateEffect() {
            if (isTriggering) {
                ticks += 1;
                if (isLocal()) player.getAbilities().setWalkingSpeed(0.07f);
                for (int i = 0; i <= 10; ++i) {
                    Particle p = MdParticleFactory.INSTANCE.next(player.level(),
                        VecUtils.add(player.position(),
                            new Vec3(RandUtils.ranged(-.3, .3), RandUtils.ranged(-.3, .3), RandUtils.ranged(-.3, .3))),
                        new Vec3(RandUtils.ranged(-.02, .02), RandUtils.ranged(-.02, .02), RandUtils.ranged(-.02, .02)));
                    player.level().addFreshEntity(p);
                }
            }
        }

        @Listener(channel=MSG_TERMINATED, side=Side.CLIENT)
        private void c_tEndEffect() {
            if (mark != null) mark.setDead();

            if (isTriggering) {
                if (isLocal()) player.getAbilities().setWalkingSpeed(0.1f);
                if (entity != null) entity.setDead();
            }
            isTriggering = false;
        }

    }

}
