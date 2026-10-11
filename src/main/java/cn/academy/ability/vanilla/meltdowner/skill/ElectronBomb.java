package cn.academy.ability.vanilla.meltdowner.skill;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.entity.EntityMdBall;
import cn.academy.entity.EntityMdRaySmall;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.NetworkS11n;
import cn.lambdalib2.s11n.network.TargetPoints;
import cn.lambdalib2.util.EntitySelectors;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.Raytrace;
import net.minecraft.world.entity.player.Player;
import cn.lambdalib2.util.RayTraceResult;
import net.minecraft.world.phys.Vec3;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;


public class ElectronBomb extends Skill {
    public static final ElectronBomb Instance = new ElectronBomb();

    private ElectronBomb() {
        super("electron_bomb", 1);
    }

    @Override
    public void activate(ClientRuntime rt, int keyID) {
        activateSingleKey2(rt, keyID, Ctx::new);
    }

    static final String MsgEffect = "effect";
    static final int Life = 20, LifeImproved = 5;
    static final double Distance = 15;

    /**
     * The point the player aims at: the centre of the looked-at entity, else the looked-at block or the end of sight.
     *  (The original aimed above the hit point by 0.6 of the target's eye height, which made the ray from the ball pass
     *  over most mobs.)
     */
    static Vec3 getDest(Player player) {
        RayTraceResult res = Raytrace.traceLiving(player, Distance, EntitySelectors.of(EntityMdBall.class).negate());
        if (res != null && res.entityHit != null) {
            return res.entityHit.getBoundingBox().getCenter();
        }
        if (res != null && res.hitVec != null) {
            return res.hitVec;
        }
        return player.getEyePosition(1f).add(player.getLookAngle().scale(Distance));
    }

    public static class Ctx extends Context<ElectronBomb> {

        public Ctx(Player _player) {
            super(_player, Instance);
        }

        @Listener(channel = MSG_MADEALIVE, side = Side.SERVER)
        private void s_Execute() {
            float exp = ctx.getSkillExp();
            EntityMdBall ball = new EntityMdBall(
                player,
                ctx.getSkillExp() > 0.8f ? LifeImproved : Life,
                target -> {
                    Vec3 from = target.getVisualPos(), dest = getDest(player);
                    // Extend a little past the aimed point so the ray ends inside the target
                    Vec3 end = dest.add(dest.subtract(from).normalize().scale(1.0));
                    RayTraceResult trace = Raytrace.perform(player.level(), from, end, EntitySelectors.exclude(player).and(EntitySelectors.of(EntityMdBall.class).negate()));
                    if (trace != null && trace.entityHit != null)
                        MDDamageHelper.attack(ctx, trace.entityHit, getDamage(exp));

                    NetworkMessage.sendToAllAround(
                        TargetPoints.convert(player, 20),
                        EffectDelegate.Instance,
                        MsgEffect,
                        target
                    );
                });
            player.level().addFreshEntity(ball);

            ctx.addSkillExp(.005f);
            ctx.setCooldown((int) MathUtils.lerpf(20, 10, exp));
            terminate();
        }

        private float getDamage(float exp) {
            return MathUtils.lerpf(6, 12, exp);
        }

    }

    public enum EffectDelegate {
        Instance;

        EffectDelegate() {}

        @StateEventCallback
        private static void preInit(StateEvents.PreInit ev) {
            NetworkS11n.addDirectInstance(Instance);
        }

        @Environment(EnvType.CLIENT)
        @Listener(channel = MsgEffect, side = Side.CLIENT)
        private void onSpawnEffect(EntityMdBall ball) {
            Player player = ball.getSpawner();
            if (player == null)
                return;
            Vec3 dest = getDest(player);
            Vec3 from = ball.getVisualPos();
            EntityMdRaySmall ray = new EntityMdRaySmall(ball.level());
            ray.setFromTo(from.x, from.y, from.z, dest.x, dest.y, dest.z);
            ray.viewOptimize = false;
            player.level().addFreshEntity(ray);
        }
    }
}
