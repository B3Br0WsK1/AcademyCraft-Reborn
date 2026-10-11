package cn.academy.ability.vanilla.meltdowner.skill;

import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.entity.EntityBarrageRayPre;
import cn.academy.entity.EntityMdRayBarrage;
import cn.academy.entity.EntitySilbarn;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.EntitySelectors;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.RayTraceResult;
import cn.lambdalib2.util.Raytrace;
import cn.lambdalib2.util.Side;
import cn.lambdalib2.util.WorldUtils;
import cn.lambdalib2.util.entityx.event.CollideEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.function.Predicate;

import static cn.lambdalib2.util.MathUtils.lerpf;
import static cn.lambdalib2.util.VecUtils.add;
import static cn.lambdalib2.util.VecUtils.multiply;

/**
 * Ray barrage. Ported from Scala.
 * @author WeAthFolD, KSkun
 */
public class RayBarrage extends Skill {

    public static final RayBarrage INSTANCE = new RayBarrage();

    private RayBarrage() {
        super("ray_barrage", 4);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        activateSingleKey(rt, keyID, RBContext::new);
    }

    public static final String MSG_START = "start";
    public static final String MSG_SYNC_SILBARN = "sync_silbarn";
    public static final String MSG_EXECUTE = "execute";

    public static final String MSG_EFFECT_PRERAY = "effect_preray";
    public static final String MSG_EFFECT_BARRAGE = "effect_barrage";

    public static class RBContext extends Context<RayBarrage> {

        private static final double DISPLAY_RAY_DIST = 20;
        private static final double RAY_DIST = 20;

        private boolean hit = false;
        private EntitySilbarn silbarn;

        public RBContext(Player p) {
            super(p, INSTANCE);
        }

        private float getPlainDamage(float exp) { return lerpf(25, 60, exp); }

        private float getScatteredDamage(float exp) { return lerpf(10, 18, exp); }

        @Listener(channel=MSG_KEYDOWN, side=Side.CLIENT)
        private void l_onKeyDown() {
            sendToServer(MSG_START);
        }

        @Listener(channel=MSG_SYNC_SILBARN, side=Side.CLIENT)
        private void c_sync_silbarn(EntitySilbarn _silbarn) {
            silbarn = _silbarn;
        }

        @Listener(channel=MSG_START, side=Side.SERVER)
        private void s_consume() {
            float exp = ctx.getSkillExp();

            RayTraceResult pos = Raytrace.traceLiving(player, DISPLAY_RAY_DIST);
            if (pos != null && pos.entityHit instanceof EntitySilbarn sb && !sb.isHit()) {
                hit = true;
                silbarn = sb;
            }

            sendToClient(MSG_SYNC_SILBARN, silbarn);

            float cp = lerpf(450, 380, exp);
            float overload = lerpf(300, 140, exp);
            if (!ctx.consume(overload, cp)) terminate();
            sendToSelf(MSG_EXECUTE);
        }

        @Listener(channel=MSG_EXECUTE, side=Side.SERVER)
        private void s_execute() {
            float exp = ctx.getSkillExp();

            double tx, ty, tz;
            if (hit) {
                if (silbarn == null) return;
                tx = silbarn.getX();
                ty = silbarn.getY();
                tz = silbarn.getZ();
                // Post the collide event to make it break. Might a bit hacking
                silbarn.postEvent(new CollideEvent(new RayTraceResult(silbarn)));
                sendToClient(MSG_EFFECT_BARRAGE, silbarn);
                // Do the damage
                float range = 55;

                float yaw = player.getYRot();
                float pitch = player.getXRot();

                float minYaw = yaw - range / 2;
                float maxYaw = yaw + range / 2;

                float minPitch = pitch - range;
                float maxPitch = pitch + range;

                Predicate<Entity> selector = EntitySelectors.exclude(silbarn, player);

                Vec3 pos = player.position();
                Vec3 mo = player.getLookAngle();

                // NB: The original passes degrees to the radian-based rotation; kept for parity.
                Vec3 v0 = player.position();
                Vec3 v1 = add(multiply(mo.yRot(minYaw).xRot(minPitch), RAY_DIST), pos);
                Vec3 v2 = add(multiply(mo.yRot(minYaw).xRot(maxPitch), RAY_DIST), pos);
                Vec3 v3 = add(multiply(mo.yRot(maxYaw).xRot(maxPitch), RAY_DIST), pos);
                Vec3 v4 = add(multiply(mo.yRot(maxYaw).xRot(minPitch), RAY_DIST), pos);

                AABB aabb = WorldUtils.minimumBounds(v0, v1, v2, v3, v4);
                List<Entity> list = WorldUtils.getEntities(player.level(), aabb, selector);

                for (Entity e : list) {
                    // Double check whether the entity is within range.
                    double dx = e.getX() - player.getX();
                    double dy = (e.getY() + e.getEyeHeight()) - (player.getY() + player.getEyeHeight());
                    double dz = e.getZ() - player.getZ();

                    float eyaw = -(float) (Math.atan2(dx, dz) * 180.0D / Math.PI);
                    float epitch = -(float) (Math.atan2(dy, Math.sqrt(dz * dz + dz * dz)) * 180.0D / Math.PI);
                    if (MathUtils.angleYawinRange(minYaw, maxYaw, eyaw) && (minPitch <= epitch && epitch <= maxPitch))
                        MDDamageHelper.attack(ctx, e, getScatteredDamage(exp));
                }
            } else {
                Pair<Vec3, RayTraceResult> pres = Raytrace.getLookingPos(player, RAY_DIST);
                Vec3 pos = pres.getLeft();
                RayTraceResult result = pres.getRight();

                tx = pos.x;
                ty = pos.y;
                tz = pos.z;

                if (result != null && result.entityHit != null)
                    MDDamageHelper.attack(ctx, result.entityHit, getPlainDamage(exp));
            }

            sendToClient(MSG_EFFECT_PRERAY, player.getX(), player.getY(), player.getZ(), tx, ty, tz, hit);

            ctx.setCooldown((int) lerpf(100, 40, exp));
            ctx.addSkillExp(.005f);
            terminate();
        }

    }

    @Environment(EnvType.CLIENT)
    @RegClientContext(RBContext.class)
    public static class RBContextC extends ClientContext {

        public RBContextC(Context par) {
            super(par);
        }

        @Listener(channel=MSG_EFFECT_PRERAY, side=Side.CLIENT)
        private void c_spawnPreRay(double x0, double y0, double z0, double x1, double y1, double z1, boolean hit) {
            EntityBarrageRayPre raySmall = new EntityBarrageRayPre(player.level(), hit);
            raySmall.setFromTo(x0, y0 + 1.6, z0, x1, y1, z1);
            player.level().addFreshEntity(raySmall);
        }

        @Listener(channel=MSG_EFFECT_BARRAGE, side=Side.CLIENT)
        private void c_spawnBarrage(EntitySilbarn silbarn) {
            player.level().addFreshEntity(new EntityMdRayBarrage(player.level(),
                silbarn.getX(), silbarn.getY(), silbarn.getZ(), player.getYRot(), player.getXRot()));
        }

    }

}
