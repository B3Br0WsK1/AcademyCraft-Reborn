package cn.academy.ability.vanilla.teleporter.skill;

import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.ability.vanilla.teleporter.util.TPSkillHelper;
import cn.academy.client.sound.ACSounds;
import cn.academy.datapart.CPData;
import cn.academy.entity.EntityTPMarking;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.RayTraceResult;
import cn.lambdalib2.util.Raytrace;
import cn.lambdalib2.util.Side;
import cn.lambdalib2.util.VecUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import static cn.lambdalib2.util.MathUtils.lerpf;

/**
 * Mark teleport. Ported from Scala.
 * @author WeAthFolD, KSkun
 */
public class MarkTeleport extends Skill {

    public static final MarkTeleport INSTANCE = new MarkTeleport();

    private MarkTeleport() {
        super("mark_teleport", 2);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        activateSingleKey(rt, keyID, MTContext::new);
    }

    public static final String MSG_EXECUTE = "execute";
    public static final String MSG_SOUND = "sound";

    public static class MTContext extends Context<MarkTeleport> {

        private static final double MINIMUM_VALID_DISTANCE = 3.0;

        private int ticks = 0;
        private final float exp = ctx.getSkillExp();

        public MTContext(Player p) {
            super(p, INSTANCE);
        }

        @Listener(channel=MSG_KEYUP, side=Side.CLIENT)
        private void l_onKeyUp() {
            sendToServer(MSG_EXECUTE);
        }

        @Listener(channel=MSG_KEYABORT, side=Side.CLIENT)
        private void l_onKeyAbort() {
            terminate();
        }

        @Listener(channel=MSG_TICK, side=Side.SERVER)
        private void s_tick() {
            ticks += 1;
        }

        @Listener(channel=MSG_EXECUTE, side=Side.SERVER)
        private void s_execute() {
            Vec3 dest = getDest(player, ticks);
            float distance = (float) dest.distanceTo(player.position());
            if (distance >= MINIMUM_VALID_DISTANCE) {
                sendToClient(MSG_SOUND);
                float overload = lerpf(40, 20, exp);
                ctx.consumeWithForce(overload, distance * getCPB(exp));
                if (player.isPassenger())
                    player.stopRiding();
                player.teleportTo(dest.x, dest.y, dest.z);
                float expincr = 0.00018f * distance;
                ctx.addSkillExp(expincr);
                player.resetFallDistance();
                ctx.setCooldown((int) lerpf(30, 0, exp));
                TPSkillHelper.incrTPCount(player);
            }
            terminate();
        }

        public double getMaxDist(float exp, float cp, int ticks) {
            double max = lerpf(25, 60, exp);
            double cplim = cp / getCPB(exp);
            return Math.min((ticks + 1) * 2, Math.min(max, cplim));
        }

        /**
         * @return Consumption per block
         */
        public float getCPB(float exp) {
            return lerpf(12, 4, exp);
        }

        public Vec3 getDest(Player player, int ticks) {
            CPData cpData = CPData.get(player);
            double dist = getMaxDist(ctx.getSkillExp(), cpData.getCP(), ticks);
            RayTraceResult mop = Raytrace.traceLiving(player, dist);
            double x, y, z;
            if (mop != null && mop.typeOfHit != RayTraceResult.Type.MISS) {
                x = mop.hitVec.x;
                y = mop.hitVec.y;
                z = mop.hitVec.z;
                if (mop.typeOfHit == RayTraceResult.Type.BLOCK) {
                    switch (mop.sideHit) {
                        case DOWN -> y -= 1.0;
                        case UP -> y += 1.8;
                        case NORTH -> { z -= .6; y = mop.getBlockPos().getY() + 1.7; }
                        case SOUTH -> { z += .6; y = mop.getBlockPos().getY() + 1.7; }
                        case WEST -> { x -= .6; y = mop.getBlockPos().getY() + 1.7; }
                        case EAST -> { x += .6; y = mop.getBlockPos().getY() + 1.7; }
                    }
                    // check head
                    if (mop.sideHit.get3DDataValue() > 1) {
                        BlockPos head = BlockPos.containing(x, y + 1, z);
                        if (!player.level().isEmptyBlock(head)) y -= 1.25;
                    }
                } else {
                    y += mop.entityHit.getEyeHeight();
                }
            } else {
                Vec3 mo = VecUtils.add(player.getEyePosition(1f), VecUtils.multiply(player.getLookAngle(), dist));
                x = mo.x;
                y = mo.y;
                z = mo.z;
            }
            return new Vec3(x, y, z);
        }

    }

    @Environment(EnvType.CLIENT)
    @RegClientContext(MTContext.class)
    public static class MTContextC extends ClientContext {

        private EntityTPMarking mark;
        private int ticks = 0;

        public MTContextC(Context par) {
            super(par);
        }

        @Listener(channel=MSG_MADEALIVE, side=Side.CLIENT)
        private void l_start() {
            if (isLocal()) {
                mark = new EntityTPMarking(player);
                player.level().addFreshEntity(mark);
            }
        }

        @Listener(channel=MSG_TICK, side=Side.CLIENT)
        private void l_update() {
            if (mark == null) {
                terminate();
                return;
            }

            ticks += 1;
            Vec3 dest = ((MTContext) parent).getDest(player, ticks);
            if (isLocal()) mark.setPos(dest.x, dest.y, dest.z);
        }

        @Listener(channel=MSG_TERMINATED, side=Side.CLIENT)
        private void l_end() {
            if (isLocal() && mark != null) mark.setDead();
        }

        @Listener(channel=MSG_SOUND, side=Side.CLIENT)
        private void c_sound() {
            ACSounds.playClient(player, "tp.tp", SoundSource.AMBIENT, .5f);
        }

    }

}
