package cn.academy.ability.vanilla.vecmanip.skill;

import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.IConsumptionProvider;
import cn.academy.ability.context.RegClientContext;
import cn.academy.ability.vanilla.vecmanip.client.effect.ParabolaEffect;
import cn.academy.client.sound.ACSounds;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.BlockSelectors;
import cn.lambdalib2.util.EntityLook;
import cn.lambdalib2.util.EntitySelectors;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.RayTraceResult;
import cn.lambdalib2.util.Raytrace;
import cn.lambdalib2.util.Side;
import cn.lambdalib2.util.VecUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import static cn.lambdalib2.util.MathUtils.clampd;
import static cn.lambdalib2.util.MathUtils.lerp;
import static cn.lambdalib2.util.MathUtils.lerpf;

/**
 * Vector acceleration. Ported from Scala.
 */
public class VecAccel extends Skill {

    public static final VecAccel INSTANCE = new VecAccel();

    private VecAccel() {
        super("vec_accel", 2);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        activateSingleKey(rt, keyID, VecAccelContext::new);
    }

    public static final String MSG_PERFORM = "perform";

    public static final double MAX_VELOCITY = 2.5;
    public static final int MAX_CHARGE = 20;
    public static final double PLAYER_ACCEL = -0.08;
    public static final double DAMPING = 0.9;
    public static final double LN_A = Math.log(DAMPING);

    public static class VecAccelContext extends Context<VecAccel> implements IConsumptionProvider {

        public int ticker = 0;

        public boolean canPerform = true;

        private final float consumption = lerpf(120, 80, ctx.getSkillExp());

        private final boolean ignoreGroundChecking = ctx.getSkillExp() > 0.5f;

        public VecAccelContext(Player p) {
            super(p, INSTANCE);
        }

        @Override
        public float getConsumptionHint() {
            return consumption;
        }

        @Listener(channel=MSG_KEYUP, side=Side.CLIENT)
        public void l_keyUp() {
            l_perform();
        }

        @Listener(channel=MSG_KEYABORT, side=Side.CLIENT)
        public void l_keyAbort() {
            terminate();
        }

        @Listener(channel=MSG_TICK, side=Side.CLIENT)
        public void l_tickHandler() {
            if (isLocal()) {
                ticker += 1;
                updateCanPerform();
            }
        }

        public void l_perform() {
            if (canPerform && consume()) {
                VecUtils.setMotion(player, initSpeed(0));
                if (player.isPassenger()) player.stopRiding();
                ctx.setCooldown((int) lerpf(80, 50, ctx.getSkillExp()));

                sendToServer(MSG_PERFORM);
            } else {
                terminate();
            }
        }

        @Listener(channel=MSG_PERFORM, side=Side.SERVER)
        public void s_perform() {
            consume();
            player.resetFallDistance();
            ctx.addSkillExp(0.002f);

            sendToClient(MSG_PERFORM);
            terminate();
        }

        public Vec3 initSpeed(float partialTicks) {
            Vec3 look = new EntityLook(
                MathUtils.lerpf(player.yRotO, player.getYRot(), partialTicks),
                MathUtils.lerpf(player.xRotO, player.getXRot(), partialTicks) - 10).toVec3();
            return VecUtils.multiply(look, speed());
        }

        private double speed() {
            double prog = lerp(0.4, 1, clampd(0, 1, (double) ticker / MAX_CHARGE));
            return Math.sin(prog) * MAX_VELOCITY;
        }

        private boolean consume() {
            float cp = consumption;
            float overload = lerpf(30, 15, ctx.getSkillExp());

            return ctx.consume(overload, cp);
        }

        private void updateCanPerform() {
            canPerform = ignoreGroundChecking || checkGround();
        }

        private boolean checkGround() {
            Vec3 p0 = player.position();
            Vec3 p1 = new Vec3(p0.x, p0.y - 2, p0.z);

            RayTraceResult traceResult = Raytrace.perform(world(), p0, p1, EntitySelectors.nothing(), BlockSelectors.filNothing);
            return traceResult != null && traceResult.typeOfHit == RayTraceResult.Type.BLOCK;
        }

    }

    @Environment(EnvType.CLIENT)
    @RegClientContext(VecAccelContext.class)
    public static class VecAccelContextC extends ClientContext {

        public VecAccelContextC(Context par) {
            super(par);
        }

        @Listener(channel=MSG_MADEALIVE, side=Side.CLIENT)
        public void l_makeAlive() {
            if (isLocal()) {
                world().addFreshEntity(new ParabolaEffect((VecAccelContext) parent));
            }
        }

        @Listener(channel=MSG_PERFORM, side=Side.CLIENT)
        public void c_perform() {
            ACSounds.playClient(player, "vecmanip.vec_accel", SoundSource.AMBIENT, 0.35f);
        }

    }

}
