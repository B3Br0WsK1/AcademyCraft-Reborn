package cn.academy.ability.vanilla.electromaster.skill;

import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.client.render.util.ArcPatterns;
import cn.academy.client.sound.ACSounds;
import cn.academy.entity.EntityArc;
import cn.lambdalib2.s11n.SerializeIncluded;
import cn.lambdalib2.s11n.SerializeNullable;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.NetworkS11nType;
import cn.lambdalib2.util.EntitySelectors;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.RayTraceResult;
import cn.lambdalib2.util.Raytrace;
import cn.lambdalib2.util.Side;
import cn.lambdalib2.util.VecUtils;
import cn.lambdalib2.util.WorldUtils;
import cn.lambdalib2.util.entityx.handlers.Life;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Predicate;

import static cn.lambdalib2.util.MathUtils.lerp;
import static cn.lambdalib2.util.MathUtils.lerpf;

/**
 * Thunder bolt. Ported from Scala.
 * @author WeAthFolD, KSkun
 */
public class ThunderBolt extends Skill {

    public static final ThunderBolt INSTANCE = new ThunderBolt();

    public static final double RANGE = 20d;
    public static final double AOE_RANGE = 8d;

    private ThunderBolt() {
        super("thunder_bolt", 4);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void activate(ClientRuntime rt, int keyId) {
        activateSingleKey(rt, keyId, ThunderBoltContext::new);
    }

    public static final String MSG_PERFORM = "perform";

    @NetworkS11nType
    public static class AttackData {
        @SerializeIncluded
        public List<Entity> aoes;
        @SerializeIncluded
        @SerializeNullable
        public Entity target;
        @SerializeIncluded
        @SerializeNullable
        public Vec3 point;
    }

    public static class ThunderBoltContext extends Context<ThunderBolt> {

        private final float exp = ctx.getSkillExp();
        private final float aoeDamage = lerpf(6, 15, exp);
        private final float damage = lerpf(10, 25, exp);

        public ThunderBoltContext(Player p) {
            super(p, INSTANCE);
        }

        private float getExpIncr(boolean effective) {
            return effective ? 0.005f : 0.003f;
        }

        private boolean consume() {
            float overload = lerpf(50, 27, exp);
            int cp = (int) lerp(280, 420, exp);
            return ctx.consume(overload, cp);
        }

        @Listener(channel=MSG_KEYDOWN, side=Side.CLIENT)
        private void l_onKeyDown() {
            sendToServer(MSG_PERFORM);
        }

        @Listener(channel=MSG_PERFORM, side=Side.SERVER)
        private void s_perform() {
            if (consume()) {
                AttackData ad = getAttackData();

                sendToClient(MSG_PERFORM, ad);

                boolean effective = false;

                if (ad.target != null) {
                    effective = true;
                    EMDamageHelper.attack(ctx, ad.target, damage);
                    if (exp > 0.2 && RandUtils.ranged(0, 1) < 0.8 && ad.target instanceof LivingEntity living) {
                        living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 3));
                    }
                }

                for (Entity e : ad.aoes) {
                    effective = true;
                    EMDamageHelper.attack(ctx, e, aoeDamage);

                    if (exp > 0.2 && RandUtils.ranged(0, 1) < 0.8 && ad.target instanceof LivingEntity living) {
                        living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 3));
                    }
                }

                ctx.addSkillExp(getExpIncr(effective));

                ctx.setCooldown((int) lerpf(120, 50, exp));
            }
            terminate();
        }

        public AttackData getAttackData() {
            RayTraceResult result = Raytrace.traceLiving(player, RANGE);
            Vec3 end;
            if (result == null || result.typeOfHit == RayTraceResult.Type.MISS) {
                end = VecUtils.lookingPos(player, RANGE);
            } else {
                end = result.hitVec;
                if (result.typeOfHit == RayTraceResult.Type.ENTITY) {
                    end = end.add(0, result.entityHit.getEyeHeight(), 0);
                }
            }

            boolean hitEntity = !(result == null || result.entityHit == null);
            Predicate<Entity> exclusion = !hitEntity ? EntitySelectors.exclude(player) : EntitySelectors.exclude(player, result.entityHit);
            Entity target = hitEntity ? result.entityHit : null;
            List<Entity> aoes = WorldUtils.getEntities(
                player.level(), end.x, end.y, end.z,
                AOE_RANGE, EntitySelectors.living().and(exclusion));

            AttackData ad = new AttackData();
            ad.aoes = aoes;
            ad.target = target;
            ad.point = end;
            return ad;
        }

    }

    @Environment(EnvType.CLIENT)
    @RegClientContext(ThunderBoltContext.class)
    public static class ThunderBoltContextC extends ClientContext {

        public ThunderBoltContextC(Context par) {
            super(par);
        }

        @Listener(channel=MSG_PERFORM, side=Side.CLIENT)
        private void c_spawnEffect(AttackData ad) {
            for (int i = 0; i <= 2; ++i) {
                EntityArc mainArc = new EntityArc(player, ArcPatterns.strongArc);
                mainArc.length = RANGE;
                player.level().addFreshEntity(mainArc);
                mainArc.addMotionHandler(new Life(20));
            }

            for (Entity e : ad.aoes) {
                EntityArc aoeArc = new EntityArc(player, ArcPatterns.aoeArc);
                aoeArc.lengthFixed = false;
                aoeArc.setFromTo(ad.point.x, ad.point.y, ad.point.z,
                    e.getX(), e.getY() + e.getEyeHeight(), e.getZ());
                aoeArc.addMotionHandler(new Life(RandUtils.rangei(15, 25)));
                player.level().addFreshEntity(aoeArc);
            }

            ACSounds.playClient(player, "em.arc_strong", SoundSource.AMBIENT, 0.6f);
        }

    }

}
