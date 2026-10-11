package cn.academy.ability.vanilla.electromaster.skill;

import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.ability.vanilla.electromaster.CatElectromaster;
import cn.academy.client.render.util.ACRenderingHelper;
import cn.academy.client.render.util.ArcPatterns;
import cn.academy.client.sound.ACSounds;
import cn.academy.client.sound.FollowEntitySound;
import cn.academy.datapart.AbilityData;
import cn.academy.entity.EntityArc;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.RayTraceResult;
import cn.lambdalib2.util.Raytrace;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import static cn.lambdalib2.util.MathUtils.lerpf;

/**
 * Magnetic movement. Ported from Scala.
 * @author WeAthFolD, KSkun
 */
public class MagMovement extends Skill {

    public static final MagMovement INSTANCE = new MagMovement();

    public static final double ACCEL = 0.08d;

    public static final String SOUND = "em.move_loop";

    private MagMovement() {
        super("mag_movement", 2);
    }

    public static double getMaxDistance(AbilityData data) {
        return 25;
    }

    public static Target toTarget(AbilityData aData, Level world, RayTraceResult pos) {
        if (pos.typeOfHit == RayTraceResult.Type.BLOCK) {
            Block block = world.getBlockState(pos.getBlockPos()).getBlock();
            if (aData.getSkillExp(INSTANCE) < 0.6f && !CatElectromaster.isMetalBlock(block)) {
                return null;
            }
            if (!CatElectromaster.isWeakMetalBlock(block) && !CatElectromaster.isMetalBlock(block)) {
                return null;
            }
            return new PointTarget(pos.hitVec.x, pos.hitVec.y, pos.hitVec.z);
        } else {
            if (pos.entityHit != null && CatElectromaster.isEntityMetallic(pos.entityHit)) {
                return new EntityTarget(pos.entityHit);
            }
            return null;
        }
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void activate(ClientRuntime rt, int keyid) {
        activateSingleKey(rt, keyid, MovementContext::new);
    }

    public static final String MSG_EFFECT_START = "effect_start";
    public static final String MSG_EFFECT_UPDATE = "effect_update";

    public static class MovementContext extends Context<MagMovement> {

        private boolean canSpawnEffect = false;

        private double mox, moy, moz;
        private final double sx, sy, sz;
        private Target target;

        private final float exp = ctx.getSkillExp();
        private final float cp = lerpf(15, 8, exp);
        private final float overload = lerpf(60, 30, exp);

        private float overloadKeep = 0f;

        private final double velocity = 1d;

        public MovementContext(Player p) {
            super(p, INSTANCE);
            sx = player.getX();
            sy = player.getY();
            sz = player.getZ();
        }

        private float getExpIncr(double distance) {
            return Math.max(0.005f, 0.0011f * (float) distance);
        }

        private double tryAdjust(double from, double to) {
            double d = to - from;
            if (Math.abs(d) < ACCEL) return to;
            return d > 0 ? from + ACCEL : from - ACCEL;
        }

        @Listener(channel=MSG_MADEALIVE, side={Side.SERVER, Side.CLIENT})
        private void g_onStart() {
            ctx.consume(overload, 0);
            overloadKeep = ctx.cpData.getOverload();
            AbilityData aData = AbilityData.get(player);
            RayTraceResult result = Raytrace.traceLiving(player, getMaxDistance(aData));
            if (result != null && result.typeOfHit != RayTraceResult.Type.MISS) {
                target = toTarget(aData, player.level(), result);
                if (target == null) {
                    terminate();
                } else {
                    canSpawnEffect = true;
                }
            } else {
                terminate();
            }
        }

        @Listener(channel=MSG_EFFECT_START, side=Side.SERVER)
        private void s_onEffectStart() {
            sendToClient(MSG_EFFECT_START);
        }

        @Listener(channel=MSG_EFFECT_UPDATE, side=Side.SERVER)
        private void s_onEffectUpdate(Vec3 vec) {
            sendToClient(MSG_EFFECT_UPDATE, vec);
        }

        @Listener(channel=MSG_TICK, side=Side.CLIENT)
        private void c_onTick() {
            if (ctx.cpData.getOverload() < overloadKeep) ctx.cpData.setOverload(overloadKeep);
            if (canSpawnEffect) {
                sendToServer(MSG_EFFECT_START);
                canSpawnEffect = false;
            }
            if (target != null) {
                target.tick();
                sendToServer(MSG_EFFECT_UPDATE, new Vec3(target.x, target.y, target.z));
                double dx = target.x - player.getX();
                double dy = target.y - player.getY();
                double dz = target.z - player.getZ();

                Vec3 motion = player.getDeltaMovement();
                double lastMo = MathUtils.lengthSq(motion.x, motion.y, motion.z);
                if (Math.abs(MathUtils.lengthSq(mox, moy, moz) - lastMo) > 0.5) {
                    mox = motion.x;
                    moy = motion.y;
                    moz = motion.z;
                }

                double mod = Math.sqrt(dx * dx + dy * dy + dz * dz) / velocity;

                dx /= mod;
                dy /= mod;
                dz /= mod;

                mox = tryAdjust(mox, dx);
                moy = tryAdjust(moy, dy);
                moz = tryAdjust(moz, dz);
                player.setDeltaMovement(mox, moy, moz);
            }
        }

        @Listener(channel=MSG_TICK, side=Side.SERVER)
        private void s_onTick() {
            if ((target != null && !target.alive()) || !ctx.consume(0, cp)) terminate();
        }

        @Listener(channel=MSG_TERMINATED, side=Side.SERVER)
        private void s_onEnd() {
            double traveledDistance = MathUtils.distance(sx, sy, sz, player.getX(), player.getY(), player.getZ());
            ctx.addSkillExp(getExpIncr(traveledDistance));

            player.resetFallDistance();
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
    @RegClientContext(MovementContext.class)
    public static class MovementContextC extends ClientContext {

        private EntityArc arc;
        private FollowEntitySound sound;

        public MovementContextC(Context par) {
            super(par);
        }

        @Listener(channel=MSG_EFFECT_START, side=Side.CLIENT)
        private void c_startEffect() {
            arc = new EntityArc(player, ArcPatterns.thinContiniousArc);
            arc.lengthFixed = false;
            arc.texWiggle = 1;
            arc.showWiggle = 0.1;
            arc.hideWiggle = 0.6;

            player.level().addFreshEntity(arc);

            sound = new FollowEntitySound(player, SOUND, SoundSource.AMBIENT).setLoop();
            ACSounds.playClient(sound);
        }

        @Listener(channel=MSG_EFFECT_UPDATE, side=Side.CLIENT)
        private void c_updateEffect(Vec3 target) {
            if (arc != null)
                arc.setFromTo(player.getX(), player.getY() + ACRenderingHelper.getHeightFix(player), player.getZ(),
                    target.x, target.y, target.z);
        }

        @Listener(channel=MSG_TERMINATED, side=Side.CLIENT)
        private void c_endEffect() {
            if (arc != null) arc.setDead();
            if (sound != null) sound.stop();
        }

    }

    public abstract static class Target {
        public double x, y, z;

        public abstract void tick();

        public abstract boolean alive();
    }

    private static class PointTarget extends Target {
        PointTarget(double _x, double _y, double _z) {
            x = _x;
            y = _y;
            z = _z;
        }

        @Override
        public void tick() {}

        @Override
        public boolean alive() {
            return true;
        }
    }

    private static class EntityTarget extends Target {
        final Entity target;

        EntityTarget(Entity _t) {
            target = _t;
        }

        @Override
        public void tick() {
            x = target.getX();
            y = target.getY() + target.getEyeHeight();
            z = target.getZ();
        }

        @Override
        public boolean alive() {
            return !target.isRemoved();
        }
    }

}
