package cn.academy.ability.vanilla.vecmanip.skill;

import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.ability.vanilla.generic.client.effect.BloodSprayEffect;
import cn.academy.client.sound.ACSounds;
import cn.academy.entity.EntityBloodSplash;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.BlockSelectors;
import cn.lambdalib2.util.EntityLook;
import cn.lambdalib2.util.EntitySelectors;
import cn.lambdalib2.util.RayTraceResult;
import cn.lambdalib2.util.Raytrace;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import static cn.lambdalib2.util.MathUtils.clampf;
import static cn.lambdalib2.util.MathUtils.lerpf;
import static cn.lambdalib2.util.RandUtils.ranged;
import static cn.lambdalib2.util.RandUtils.rangef;
import static cn.lambdalib2.util.RandUtils.rangei;

/**
 * Blood retrograde. Ported from Scala.
 */
public class BloodRetrograde extends Skill {

    public static final BloodRetrograde INSTANCE = new BloodRetrograde();

    private BloodRetrograde() {
        super("blood_retro", 4);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        activateSingleKey(rt, keyID, BloodRetroContext::new);
    }

    public static final String MSG_PERFORM = "perform";

    public static class BloodRetroContext extends Context<BloodRetrograde> {

        int tick = 0;

        private final float damage = lerpf(30, 60, ctx.getSkillExp());

        public BloodRetroContext(Player p) {
            super(p, INSTANCE);
        }

        @Listener(channel=MSG_KEYUP, side=Side.CLIENT)
        public void l_keyUp() {
            RayTraceResult trace = Raytrace.traceLiving(player, 2);
            if (trace != null && trace.typeOfHit == RayTraceResult.Type.ENTITY && trace.entityHit instanceof LivingEntity le) {
                sendToServer(MSG_PERFORM, le);
            } else {
                terminate();
            }
        }

        @Listener(channel=MSG_PERFORM, side=Side.SERVER)
        public void s_perform(LivingEntity targ) {
            if (targ != null && consume()) {
                ctx.setCooldown((int) lerpf(90, 40, ctx.getSkillExp()));
                sendToClient(MSG_PERFORM, targ);
                ctx.attack(targ, damage);
                ctx.addSkillExp(0.002f);
            }

            terminate();
        }

        @Listener(channel=MSG_TICK, side=Side.CLIENT)
        public void l_tick() {
            if (!isLocal()) return;
            tick += 1;

            player.getAbilities().setWalkingSpeed(lerpf(0.1f, 0.007f, clampf(0, 1, tick / 20.0f)));

            if (tick >= 30.0f) {
                l_keyUp();
            }
        }

        @Listener(channel=MSG_TERMINATED, side=Side.CLIENT)
        public void l_terminate() {
            if (isLocal()) {
                player.getAbilities().setWalkingSpeed(0.1f);
            }
        }

        private boolean consume() {
            float overload = lerpf(55, 40, ctx.getSkillExp());
            float consumption = lerpf(280, 350, ctx.getSkillExp());

            return ctx.consume(overload, consumption);
        }

    }

    @Environment(EnvType.CLIENT)
    @RegClientContext(BloodRetroContext.class)
    public static class BloodRetroContextC extends ClientContext {

        public BloodRetroContextC(Context par) {
            super(par);
        }

        @Listener(channel=MSG_PERFORM, side=Side.CLIENT)
        private void c_perform(LivingEntity targ) {
            if (targ == null) return;
            Vec3 look = player.getLookAngle();
            int n = rangei(6, 10);
            for (int i = 0; i < n; ++i) {
                EntityBloodSplash splash = new EntityBloodSplash(world());
                splash.setSize(rangef(1.4f, 1.8f));
                Vec3 dv = new Vec3(ranged(-1, 1) * targ.getBbWidth(), ranged(0, 1) * targ.getBbHeight(),
                    ranged(-1, 1) * targ.getBbWidth());
                splash.setPos(targ.getX() + dv.x + look.x * 0.2,
                    targ.getY() + dv.y + look.y * 0.2,
                    targ.getZ() + dv.z + look.z * 0.2);

                world().addFreshEntity(splash);
            }

            Vec3 headPos = targ.position().add(0, targ.getBbHeight() * 0.6, 0);

            for (int angle : new int[] { 0, 30, 45, 60, 80, -30, -45, -60, -80 }) {
                Vec3 dir = new EntityLook(player.getYHeadRot() + rangef(-20, 20), angle).toVec3();
                RayTraceResult r = Raytrace.perform(world(),
                    new Vec3(headPos.x - dir.x * 0.5, headPos.y - dir.y * 0.5, headPos.z - dir.z * 0.5),
                    new Vec3(headPos.x + dir.x * 5, headPos.y + dir.y * 5, headPos.z + dir.z * 5),
                    EntitySelectors.nothing(), BlockSelectors.filNormal);
                if (r != null && r.typeOfHit == RayTraceResult.Type.BLOCK) {
                    int cnt = rangei(2, 3);
                    for (int j = 0; j < cnt; ++j) {
                        world().addFreshEntity(new BloodSprayEffect(world(), r.getBlockPos(), r.sideHit.get3DDataValue()));
                    }
                }
            }
            ACSounds.playClient(player, "vecmanip.blood_retro", SoundSource.AMBIENT, 1F);
        }

    }

}
