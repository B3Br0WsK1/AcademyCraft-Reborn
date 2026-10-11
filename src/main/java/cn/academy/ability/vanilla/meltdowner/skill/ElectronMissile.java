package cn.academy.ability.vanilla.meltdowner.skill;

import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.client.render.particle.MdParticleFactory;
import cn.academy.client.render.util.ACRenderingHelper;
import cn.academy.entity.EntityMdBall;
import cn.academy.entity.EntityMdRaySmall;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.EntitySelectors;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.Side;
import cn.lambdalib2.util.VecUtils;
import cn.lambdalib2.util.WorldUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static cn.lambdalib2.util.MathUtils.lerpf;
import static cn.lambdalib2.util.RandUtils.nextInt;
import static cn.lambdalib2.util.RandUtils.ranged;
import static cn.lambdalib2.util.RandUtils.rangei;

/**
 * Electron missile. Ported from Scala.
 * @author WeAthFolD, KSkun
 */
public class ElectronMissile extends Skill {

    public static final ElectronMissile INSTANCE = new ElectronMissile();

    private ElectronMissile() {
        super("electron_missile", 5);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        activateSingleKey(rt, keyID, EMContext::new);
    }

    public static final String MSG_EFFECT_SPAWN = "effect_spawn";
    public static final String MSG_EFFECT_UPDATE = "effect_update";

    public static class EMContext extends Context<ElectronMissile> {

        private static final int MAX_HOLD = 5;

        private LinkedList<EntityMdBall> active = new LinkedList<>();
        private int ticks = 0;

        private final float exp = ctx.getSkillExp();

        private final float consumption = lerpf(12, 5, exp);
        private final float overload_attacked = lerpf(9, 4, exp);
        private final float consumption_attacked = lerpf(60, 25, exp);
        private final float overload_keep = 200;

        private float overloadKeep = 0f;

        public EMContext(Player p) {
            super(p, INSTANCE);
        }

        @Listener(channel=MSG_KEYUP, side=Side.CLIENT)
        private void l_onEnd() {
            terminate();
        }

        @Listener(channel=MSG_KEYABORT, side=Side.CLIENT)
        private void l_onAbort() {
            terminate();
        }

        @Listener(channel=MSG_MADEALIVE, side=Side.SERVER)
        private void s_madeAlive() {
            ctx.consume(overload_keep, 0);
            overloadKeep = ctx.cpData.getOverload();
            active = new LinkedList<>();
        }

        @Listener(channel=MSG_TICK, side=Side.SERVER)
        private void s_onTick() {
            if (ctx.cpData.getOverload() < overloadKeep) ctx.cpData.setOverload(overloadKeep);
            if (!ctx.consume(0, consumption)) {
                terminate();
                return;
            }
            int timeLimit = (int) lerpf(80, 200, exp);
            if (ticks <= timeLimit) {
                if (ticks % 10 == 0 && active.size() < MAX_HOLD) {
                    EntityMdBall ball = new EntityMdBall(player);
                    player.level().addFreshEntity(ball);
                    active.add(ball);
                }
                if (ticks != 0 && ticks % 8 == 0) {
                    float range = lerpf(5, 13, exp);
                    List<Entity> list = WorldUtils.getEntities(player, range,
                        EntitySelectors.exclude(player).and(EntitySelectors.living()));
                    if (!active.isEmpty() && !list.isEmpty() && ctx.consume(overload_attacked, consumption_attacked)) {
                        double min = Double.MAX_VALUE;
                        Entity result = null;
                        for (Entity e : list) {
                            double dist = e.distanceToSqr(player);
                            if (dist < min) {
                                min = dist;
                                result = e;
                            }
                        }
                        // Find a random ball and destroy it
                        int index = nextInt(active.size());
                        Iterator<EntityMdBall> iter = active.iterator();
                        EntityMdBall ball = null;
                        for (int i = 0; i <= index; ++i) ball = iter.next();
                        iter.remove();
                        // client action
                        sendToClient(MSG_EFFECT_SPAWN, ball.getVisualPos(), VecUtils.add(VecUtils.entityPos(result),
                            new Vec3(0, result.getEyeHeight(), 0)));
                        // server action
                        result.setInvulnerableTime(-1);
                        float damage = lerpf(10, 18, exp);
                        MDDamageHelper.attack(ctx, result, damage);
                        ctx.addSkillExp(0.001f);
                        ball.setDead();
                    }
                }
            } else {
                terminate();
            }
            sendToClient(MSG_EFFECT_UPDATE);
            ticks += 1;
        }

        @Listener(channel=MSG_TERMINATED, side=Side.SERVER)
        private void s_onEnd() {
            int cooldown = MathUtils.clampi(700, 400, (int) exp);
            ctx.setCooldown(cooldown);

            for (EntityMdBall ball : active) {
                ball.setDead();
            }
        }

    }

    @Environment(EnvType.CLIENT)
    @RegClientContext(EMContext.class)
    public static class EMContextC extends ClientContext {

        public EMContextC(Context par) {
            super(par);
        }

        @Listener(channel=MSG_EFFECT_UPDATE, side=Side.CLIENT)
        private void c_updateEffect() {
            int count = rangei(1, 3);
            while (count-- > 0) {
                double r = ranged(0.5, 1);
                double theta = ranged(0, Math.PI * 2);
                double h = ranged(-1.2, 0);
                Vec3 pos = VecUtils.add(new Vec3(player.getX(), player.getY() + ACRenderingHelper.getHeightFix(player),
                    player.getZ()), new Vec3(r * Math.sin(theta), h, r * Math.cos(theta)));
                Vec3 vel = new Vec3(ranged(-.02, .02), ranged(.01, .05), ranged(-.02, .02));
                player.level().addFreshEntity(MdParticleFactory.INSTANCE.next(player.level(), pos, vel));
            }
        }

        @Listener(channel=MSG_EFFECT_SPAWN, side=Side.CLIENT)
        private void c_spawnRay(Vec3 from, Vec3 to) {
            EntityMdRaySmall ray = new EntityMdRaySmall(player.level());
            ray.setFromTo(from, to);
            player.level().addFreshEntity(ray);
        }

    }

}
