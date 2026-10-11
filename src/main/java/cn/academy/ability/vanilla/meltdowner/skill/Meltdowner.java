package cn.academy.ability.vanilla.meltdowner.skill;

import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.client.render.particle.MdParticleFactory;
import cn.academy.client.render.util.ACRenderingHelper;
import cn.academy.client.sound.ACSounds;
import cn.academy.client.sound.FollowEntitySound;
import cn.academy.entity.EntityMDRay;
import cn.academy.util.RangedRayDamage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.RayTraceResult;
import cn.lambdalib2.util.Raytrace;
import cn.lambdalib2.util.Side;
import cn.lambdalib2.util.VecUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import static cn.lambdalib2.util.MathUtils.lerpf;
import static cn.lambdalib2.util.RandUtils.ranged;
import static cn.lambdalib2.util.RandUtils.rangei;

/**
 * Meltdowner. Ported from Scala.
 * @author WeAthFolD
 */
public class Meltdowner extends Skill {

    public static final Meltdowner INSTANCE = new Meltdowner();

    private Meltdowner() {
        super("meltdowner", 3);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        activateSingleKey(rt, keyID, MDContext::new);
    }

    public static final String MSG_PERFORM = "perform";
    public static final String MSG_REFLECTED = "reflect";
    public static final int TICKS_MIN = 20;
    public static final int TICKS_MAX = 40;
    public static final int TICKS_TOLE = 100;

    public static class MDContext extends Context<Meltdowner> {

        private int ticks = 0;
        private final float exp = ctx.getSkillExp();
        private final float tickConsumption = lerpf(10, 15, exp);

        private float overloadKeep = 0f;

        public MDContext(Player p) {
            super(p, INSTANCE);
        }

        @Listener(channel=MSG_MADEALIVE, side=Side.SERVER)
        private void s_madeAlive() {
            float overload = lerpf(200, 170, exp);
            ctx.consume(overload, 0);
            overloadKeep = ctx.cpData.getOverload();
        }

        @Listener(channel=MSG_KEYUP, side=Side.CLIENT)
        private void l_keyUp() {
            if (ticks >= TICKS_MIN) sendToServer(MSG_PERFORM);
            else terminate();
        }

        @Listener(channel=MSG_KEYABORT, side=Side.CLIENT)
        private void l_keyAbort() {
            terminate();
        }

        @Listener(channel=MSG_TICK, side={Side.CLIENT, Side.SERVER})
        private void g_tick() {
            ticks += 1;
            if (!isRemote()) {
                if (ctx.cpData.getOverload() < overloadKeep) ctx.cpData.setOverload(overloadKeep);
                if (!ctx.consume(0, tickConsumption) || ticks > TICKS_TOLE) terminate();
            }
        }

        @Listener(channel=MSG_PERFORM, side=Side.SERVER)
        private void s_perform() {
            int ct = toChargeTicks();
            double[] length = { 30 };
            RangedRayDamage rrd = new RangedRayDamage.Reflectible(ctx, lerpf(2, 3, exp), getEnergy(ct), reflector -> {
                length[0] = Math.min(length[0], reflector.distanceToSqr(ctx.player));
                s_reflected(reflector);
                sendToClient(MSG_REFLECTED, reflector);
            });
            rrd.startDamage = getDamage(ct);
            rrd.perform();
            ctx.addSkillExp(getExpIncr(ct));
            ctx.setCooldown(getCooldown(ct));
            sendToClient(MSG_PERFORM, ct, length[0]);
            terminate();
        }

        private void s_reflected(Entity reflector) {
            RayTraceResult result = Raytrace.traceLiving(reflector, 10);
            if (result != null && result.typeOfHit == RayTraceResult.Type.ENTITY)
                ctx.attack(result.entityHit, 0.5f * lerpf(20, 50, exp));
        }

        private float timeRate(int ct) { return MathUtils.lerpf(0.8f, 1.2f, (ct - 20.0f) / 20.0f); }

        private float getEnergy(int ct) { return timeRate(ct) * MathUtils.lerpf(300, 700, exp); }

        private float getDamage(int ct) { return timeRate(ct) * MathUtils.lerpf(18, 50, exp); }

        private int getCooldown(int ct) { return (int) (timeRate(ct) * 20 * MathUtils.lerpf(15, 7, exp)); }

        private float getExpIncr(int ct) { return timeRate(ct) * 0.002f; }

        private int toChargeTicks() { return Math.min(ticks, TICKS_MAX); }

    }

    @Environment(EnvType.CLIENT)
    @RegClientContext(MDContext.class)
    public static class MDContextC extends ClientContext {

        private int ticks = 0;

        private FollowEntitySound sound;

        public MDContextC(Context par) {
            super(par);
        }

        @Listener(channel=MSG_PERFORM, side=Side.CLIENT)
        private void c_perform(int ct, double length) {
            EntityMDRay ray = new EntityMDRay(player, length);
            ACSounds.playClient(player, "md.meltdowner", SoundSource.PLAYERS, 0.5f);
            player.level().addFreshEntity(ray);
        }

        @Listener(channel=MSG_REFLECTED, side=Side.CLIENT)
        private void c_reflected(Entity reflector) {
            Vec3 playerLook = player.getLookAngle().normalize();
            double distance = VecUtils.entityHeadPos(player).distanceTo(VecUtils.entityHeadPos(reflector));
            Vec3 spawnPos = VecUtils.add(VecUtils.entityHeadPos(player), VecUtils.multiply(playerLook, distance));
            EntityMDRay ray = new EntityMDRay(player, 10);
            ray.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            player.level().addFreshEntity(ray);
        }

        @Listener(channel=MSG_TERMINATED, side=Side.CLIENT)
        private void c_terminate() {
            if (isLocal()) player.getAbilities().setWalkingSpeed(0.1f);
            if (sound != null) sound.stop();
        }

        @Listener(channel=MSG_MADEALIVE, side=Side.CLIENT)
        private void c_start() {
            sound = new FollowEntitySound(player, "md.md_charge", SoundSource.AMBIENT).setVolume(1.0f);
            ACSounds.playClient(sound);
        }

        @Listener(channel=MSG_TICK, side=Side.CLIENT)
        private void c_tick() {
            ticks += 1;
            if (isLocal()) player.getAbilities().setWalkingSpeed(0.1f - ticks * 0.001f);
            // Particles surrounding player
            int count = rangei(2, 3);
            while (count-- > 0) {
                double r = ranged(0.7, 1);
                double theta = ranged(0, Math.PI * 2);
                double h = ranged(-1.2, 0);
                Vec3 pos = VecUtils.add(new Vec3(player.getX(),
                    player.getY() + (ACRenderingHelper.isThePlayer(player) ? 0 : 1.6), player.getZ()),
                    new Vec3(r * Math.sin(theta), h, r * Math.cos(theta)));
                Vec3 vel = new Vec3(ranged(-.03, .03), ranged(.01, .05), ranged(-.03, .03));
                player.level().addFreshEntity(MdParticleFactory.INSTANCE.next(player.level(), pos, vel));
            }
        }

    }

}
