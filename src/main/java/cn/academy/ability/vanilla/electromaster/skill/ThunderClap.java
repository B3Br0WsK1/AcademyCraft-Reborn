package cn.academy.ability.vanilla.electromaster.skill;

import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.entity.EntityRippleMark;
import cn.academy.entity.EntitySurroundArc;
import cn.academy.entity.EntitySurroundArc.ArcType;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.EntitySelectors;
import cn.lambdalib2.util.RayTraceResult;
import cn.lambdalib2.util.Raytrace;
import cn.lambdalib2.util.Side;
import cn.lambdalib2.util.VecUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import static cn.lambdalib2.util.MathUtils.lerpf;

/**
 * Thunder clap. Ported from Scala.
 * @author WeAthFolD, KSkun
 */
public class ThunderClap extends Skill {

    public static final ThunderClap INSTANCE = new ThunderClap();

    public static final int MIN_TICKS = 40;
    public static final int MAX_TICKS = 60;

    private ThunderClap() {
        super("thunder_clap", 5);
    }

    public static float getDamage(float exp, int ticks) {
        return lerpf(36, 72, exp) * lerpf(1.0f, 1.2f, (ticks - 40.0f) / 60.0f);
    }

    public static float getRange(float exp) {
        return lerpf(15, 30, exp);
    }

    public static int getCooldown(float exp, int ticks) {
        return (int) (ticks * lerpf(10, 6, exp));
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void activate(ClientRuntime rt, int keyid) {
        activateSingleKey(rt, keyid, ThunderClapContext::new);
    }

    public static final String MSG_START = "start";
    public static final String MSG_END = "end";
    public static final String MSG_EFFECT_START = "effect_start";

    private static Vec3 traceHit(Player player) {
        final double DISTANCE = 40.0;
        RayTraceResult pos = Raytrace.traceLiving(player, DISTANCE, EntitySelectors.nothing());
        if (pos != null && pos.typeOfHit != RayTraceResult.Type.MISS) {
            return pos.hitVec;
        }
        return VecUtils.lookingPos(player, DISTANCE);
    }

    public static class ThunderClapContext extends Context<ThunderClap> {

        final float exp = ctx.getSkillExp();
        int ticks = 0;
        double hitX, hitY, hitZ;

        public ThunderClapContext(Player p) {
            super(p, INSTANCE);
        }

        @Listener(channel=MSG_KEYDOWN, side=Side.CLIENT)
        private void l_onKeyDown() {
            sendToServer(MSG_START);
        }

        @Listener(channel=MSG_START, side=Side.SERVER)
        private void s_onStart() {
            sendToClient(MSG_EFFECT_START);

            float overload = lerpf(390, 252, exp);
            ctx.consume(overload, 0);
        }

        @Listener(channel=MSG_TICK, side=Side.SERVER)
        private void s_onTick() {
            Vec3 hit = traceHit(player);
            hitX = hit.x;
            hitY = hit.y;
            hitZ = hit.z;

            ticks += 1;

            float consumption = lerpf(18, 25, exp);
            if ((ticks <= MIN_TICKS && !ctx.consume(0, consumption)) || ticks >= MAX_TICKS)
                sendToSelf(MSG_END);
        }

        @Listener(channel=MSG_END, side=Side.SERVER)
        private void s_onEnd() {
            if (ticks < MIN_TICKS) {
                terminate();
                return;
            }

            LightningBolt lightning = EntityTypes.LIGHTNING_BOLT.create(player.level(), EntitySpawnReason.TRIGGERED);
            if (lightning != null) {
                lightning.snapTo(hitX, hitY, hitZ);
                lightning.setVisualOnly(true);
                player.level().addFreshEntity(lightning);
            }
            ctx.attackRange(hitX, hitY, hitZ, ThunderClap.getRange(exp), getDamage(exp, ticks), EntitySelectors.exclude(player));

            ctx.setCooldown(getCooldown(exp, ticks));
            ctx.addSkillExp(0.003f);
            terminate();
        }

        @Listener(channel=MSG_KEYUP, side=Side.CLIENT)
        private void l_onEnd() {
            terminate();
        }

        @Listener(channel=MSG_KEYABORT, side=Side.CLIENT)
        private void l_onAbort() {
            terminate();
        }

    }

    @Environment(EnvType.CLIENT)
    @RegClientContext(ThunderClapContext.class)
    public static class ThunderClapContextC extends ClientContext {

        EntitySurroundArc surroundArc;
        EntityRippleMark mark;
        int ticks = 0;
        double hitX, hitY, hitZ;
        boolean canTicking = false;

        public ThunderClapContextC(Context par) {
            super(par);
        }

        @Listener(channel=MSG_EFFECT_START, side=Side.CLIENT)
        private void c_spawnEffect() {
            canTicking = true;
            surroundArc = new EntitySurroundArc(player).setArcType(ArcType.BOLD);
            player.level().addFreshEntity(surroundArc);

            if (isLocal()) {
                mark = new EntityRippleMark(player.level());

                player.level().addFreshEntity(mark);
                mark.color.set(204, 204, 204, 179);
                mark.setPos(hitX, hitY, hitZ);
            }
        }

        @Listener(channel=MSG_TICK, side=Side.CLIENT)
        private void c_updateEffect() {
            if (canTicking) {
                Vec3 hit = traceHit(player);
                hitX = hit.x;
                hitY = hit.y;
                hitZ = hit.z;

                ticks += 1;
                if (isLocal()) {
                    float max = 0.1f;
                    float min = 0.001f;
                    player.getAbilities().setWalkingSpeed(Math.max(min, max - (max - min) / 60 * ticks));
                    if (mark != null) mark.setPos(hitX, hitY, hitZ);
                }
            }
        }

        @Listener(channel=MSG_TERMINATED, side=Side.CLIENT)
        private void c_terminated() {
            canTicking = false;
            player.getAbilities().setWalkingSpeed(0.1f);
            if (surroundArc != null)
                surroundArc.executeAfter(target -> target.discard(), 10);

            if (isLocal() && mark != null) {
                mark.setDead();
            }
        }

    }

}
