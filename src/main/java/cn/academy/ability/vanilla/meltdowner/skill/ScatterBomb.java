package cn.academy.ability.vanilla.meltdowner.skill;

import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.entity.EntityMdBall;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.TargetPoints;
import cn.lambdalib2.util.EntitySelectors;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.RayTraceResult;
import cn.lambdalib2.util.Raytrace;
import cn.lambdalib2.util.Side;
import cn.lambdalib2.util.VecUtils;
import cn.lambdalib2.util.WorldUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static cn.lambdalib2.util.MathUtils.lerpf;

/**
 * Scatter bomb. Ported from Scala.
 * @author WeAthFolD, KSkun, Paindar
 */
public class ScatterBomb extends Skill {

    public static final ScatterBomb INSTANCE = new ScatterBomb();

    private ScatterBomb() {
        super("scatter_bomb", 2);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        activateSingleKey(rt, keyID, SBContext::new);
    }

    public static class SBContext extends Context<ScatterBomb> {

        private static final int MOD = 10;
        private static final double RAY_RANGE = 15;

        private final List<EntityMdBall> balls = new ArrayList<>();
        private final Predicate<Entity> basicSelector = EntitySelectors.everything();
        private int ticks = 0;
        private final float exp = ctx.getSkillExp();

        private float overloadKeep = 0f;

        public SBContext(Player p) {
            super(p, INSTANCE);
        }

        private float getDamage(float exp) { return lerpf(5, 9, exp); }

        @Listener(channel=MSG_KEYUP, side=Side.CLIENT)
        private void l_onKeyUp() {
            terminate();
        }

        @Listener(channel=MSG_KEYABORT, side=Side.CLIENT)
        private void l_onAbort() {
            terminate();
        }

        @Listener(channel=MSG_MADEALIVE, side=Side.SERVER)
        private void s_onStart() {
            float overload = lerpf(80, 60, exp);
            ctx.consume(overload, 0);
            overloadKeep = ctx.cpData.getOverload();
        }

        @Listener(channel=MSG_TICK, side=Side.SERVER)
        private void s_onTick() {
            if (ctx.cpData.getOverload() < overloadKeep) ctx.cpData.setOverload(overloadKeep);
            ticks += 1;

            if (ticks <= 80) {
                if (ticks >= 20 && ticks % MOD == 0) {
                    EntityMdBall ball = new EntityMdBall(player);
                    world().addFreshEntity(ball);
                    balls.add(ball);
                }
                float cp = lerpf(3, 6, exp);
                if (!ctx.consume(0, cp)) terminate();
            }
            if (ticks == 200) {
                if (player.level() instanceof ServerLevel sl)
                    player.hurtServer(sl, player.damageSources().playerAttack(player), 6);
                terminate();
            }
        }

        @Listener(channel=MSG_TERMINATED, side=Side.SERVER)
        private void s_onEnd() {
            int autoCount = exp > 0.5 ? (int) (balls.size() * exp) : 0;
            List<Entity> autoTarget = exp > 0.5
                ? WorldUtils.getEntities(player, 5, EntitySelectors.exclude(player).and(t -> t instanceof Mob))
                : new ArrayList<>();

            for (EntityMdBall ball : balls) {
                Vec3 dest = newDest();
                if (autoCount > 0 && !autoTarget.isEmpty()) {
                    Entity target = autoTarget.get(RandUtils.nextInt(autoTarget.size()));
                    dest = target.getBoundingBox().getCenter();
                    autoCount -= 1;
                }

                Vec3 from = ball.getVisualPos();
                RayTraceResult traceResult = Raytrace.perform(world(),
                    from,
                    dest.add(dest.subtract(from).normalize().scale(1.0)), basicSelector.and(EntitySelectors.exclude(player)));
                if (traceResult != null && traceResult.entityHit != null) {
                    traceResult.entityHit.setInvulnerableTime(-1);
                    MDDamageHelper.attack(ctx, traceResult.entityHit, getDamage(exp));
                }
                NetworkMessage.sendToAllAround(
                    TargetPoints.convert(player, 25),
                    SBNetDelegate.INSTANCE,
                    SBNetDelegate.MSG_EFFECT,
                    from,
                    dest
                );
                ball.setDead();
            }
            ctx.addSkillExp(0.001f * balls.size());
        }

        private Vec3 newDest() {
            Vec3 begin = VecUtils.lookingPos(player, RAY_RANGE);
            Vec3 look = player.getLookAngle()
                .xRot(MathUtils.toRadians((RandUtils.nextFloat() - 0.5F) * 25))
                .yRot(MathUtils.toRadians((RandUtils.nextFloat() - 0.5F) * 25));

            return begin.add(look.scale(RAY_RANGE));
        }

    }

}
