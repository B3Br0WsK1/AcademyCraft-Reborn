package cn.academy.ability.vanilla.vecmanip.skill;

import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.ClientRuntime.ActivateHandlers;
import cn.academy.ability.context.ClientRuntime.IActivateHandler;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.ability.ctrl.KeyDelegates;
import cn.academy.ability.vanilla.vecmanip.client.effect.WaveEffect;
import cn.academy.ability.vanilla.vecmanip.client.effect.WaveEffectUI;
import cn.academy.ability.vanilla.vecmanip.skill.EntityAffection.Affected;
import cn.academy.client.sound.ACSounds;
import cn.academy.event.ability.ReflectEvent;
import cn.lambdalib2.auxgui.AuxGui;
import cn.lambdalib2.event.EventHub;
import cn.lambdalib2.event.SubscribeEvent;
import cn.lambdalib2.event.mc.LivingEvents.LivingAttackEvent;
import cn.lambdalib2.event.mc.LivingEvents.LivingHurtEvent;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.Raytrace;
import cn.lambdalib2.util.ScaledResolution;
import cn.lambdalib2.util.Side;
import cn.lambdalib2.util.SideUtils;
import cn.lambdalib2.util.WorldUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import static cn.lambdalib2.util.MathUtils.lerpf;
import static cn.lambdalib2.util.RandUtils.ranged;
import static cn.lambdalib2.util.VecUtils.add;
import static cn.lambdalib2.util.VecUtils.entityHeadPos;
import static cn.lambdalib2.util.VecUtils.multiply;
import static cn.lambdalib2.util.VecUtils.setMotion;
import static cn.lambdalib2.util.VecUtils.subtract;

/**
 * Vector reflection. Ported from Scala.
 */
public class VecReflection extends Skill {

    public static final VecReflection INSTANCE = new VecReflection();

    private VecReflection() {
        super("vec_reflection", 4);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        rt.addKey(keyID, KeyDelegates.contextActivate(this, VecReflectionContext.class, VecReflectionContext::new));
    }

    public static final String MSG_EFFECT = "effect";
    public static final String MSG_REFLECT_ENTITY = "reflect_ent";

    static void reflect(Entity entity, Player player) {
        Vec3 lookPos = Raytrace.getLookingPos(player, 20).getLeft();
        double speed = entity.getDeltaMovement().length();
        Vec3 vel = multiply(subtract(lookPos, entityHeadPos(entity)).normalize(), speed);
        entity.setDeltaMovement(vel);
        entity.needsSync = true;
    }

    public static class VecReflectionContext extends Context<VecReflection> {

        private final Set<Entity> visited = Collections.newSetFromMap(new WeakHashMap<>());

        private final float overloadToKeep = lerpf(350, 250, ctx.getSkillExp());
        private float overloadKeep = 0f;

        // Sometimes reflection will cause reentrant, e.g. when Guardian
        //   gives thorns damage to any of its attacks, or
        //   two players vector-reflect against each other.
        // Under these situation, we don't allow recursion of reflection.
        private boolean _isAttacking = false;

        public VecReflectionContext(Player p) {
            super(p, INSTANCE);
        }

        @Listener(channel=MSG_MADEALIVE, side=Side.SERVER)
        public void s_makeAlive() {
            EventHub.BUS.register(this);
            ctx.consume(overloadToKeep, 0);
            overloadKeep = ctx.cpData.getOverload();
        }

        @Listener(channel=MSG_TERMINATED, side={Side.SERVER, Side.CLIENT})
        public void g_terminate() {
            EventHub.BUS.unregister(this);
        }

        @Listener(channel=MSG_TICK, side=Side.SERVER)
        public void s_tick() {
            if (ctx.cpData.getOverload() < overloadKeep) ctx.cpData.setOverload(overloadKeep);
            double range = 4;
            List<Entity> entities = WorldUtils.getEntities(player, range, t -> true);
            entities.removeAll(visited);

            for (Entity entity : entities) {
                if (EntityAffection.isMarked(entity)) continue;
                if (!(EntityAffection.getAffectInfo(entity) instanceof Affected affected)) continue;
                float difficulty = affected.difficulty();
                if (entity instanceof Fireball fireball) {
                    if (consumeEntity(difficulty)) {
                        createNewFireball(fireball);

                        ctx.addSkillExp(difficulty * 0.0008f);
                        sendToClient(MSG_REFLECT_ENTITY, entity);
                    }
                } else {
                    if (consumeEntity(difficulty)) {
                        reflect(entity, player);

                        EntityAffection.mark(entity);

                        ctx.addSkillExp(difficulty * 0.0008f);
                        sendToClient(MSG_REFLECT_ENTITY, entity);
                    }
                }
            }

            visited.addAll(entities);

            if (!consumeNormal())
                terminate();
        }

        private void createNewFireball(Fireball source) {
            source.discard();

            Entity shooter = source.getOwner();
            LivingEntity shootingEntity = shooter instanceof LivingEntity le ? le : null;
            Fireball fireball;

            if (source instanceof LargeFireball l && shootingEntity != null) {
                LargeFireball nf = new LargeFireball(world(), shootingEntity, Vec3.ZERO, VecDeviation.explosionPower(l));
                fireball = nf;
            } else if (source instanceof LargeFireball l) {
                LargeFireball nf = new LargeFireball(net.minecraft.world.entity.EntityTypes.FIREBALL, world());
                VecDeviation.setExplosionPower(nf, VecDeviation.explosionPower(l));
                fireball = nf;
            } else if (shootingEntity == null) {
                fireball = new SmallFireball(world(), source.getX(), source.getY(), source.getZ(), Vec3.ZERO);
            } else {
                fireball = new SmallFireball(world(), shootingEntity, Vec3.ZERO);
            }

            fireball.setPos(source.getX(), source.getY(), source.getZ());
            Vec3 lookPos = Raytrace.getLookingPos(player, 20).getLeft();
            double speed = source.getDeltaMovement().length();
            Vec3 vel = multiply(subtract(lookPos, entityHeadPos(source)).normalize(), speed);
            setMotion(fireball, vel);
            EntityAffection.mark(fireball);
            world().addFreshEntity(fireball);
        }

        @Listener(channel=MSG_TICK, side=Side.CLIENT)
        public void c_tick() {
            if (!consumeNormal())
                terminate();
        }

        @SubscribeEvent
        public void onReflect(ReflectEvent evt) {
            if (evt.target.equals(player)) {
                evt.setCanceled(true);

                Vec3 dpos = subtract(entityHeadPos(evt.player), entityHeadPos(player));
                sendToClient(MSG_EFFECT, add(add(player.position(), new Vec3(0, ranged(0.4, 1.3), 0)),
                    multiply(dpos.normalize(), 0.5)));
            }
        }

        /**
         * Note: Canceling the damage event in `LivingHurtEvent` still causes knockback, so there needs
         *  to be one more pre testing.
         */
        @SubscribeEvent
        public void onLivingAttack(LivingAttackEvent evt) {
            if (evt.getEntityLiving().equals(player)) {
                if (handleAttack(evt.getSource(), evt.getAmount(), true).performed) {
                    handleAttack(evt.getSource(), evt.getAmount(), false);
                    evt.setCanceled(true);
                }
            }
        }

        @SubscribeEvent
        public void onLivingHurt(LivingHurtEvent evt) {
            if (evt.getEntityLiving().equals(player) && evt.getAmount() <= 9999) {
                float dmg = handleAttack(evt.getSource(), evt.getAmount(), false).damage;
                evt.setAmount(dmg);
                if (dmg <= 0) {
                    evt.setCanceled(true);
                }
            }
        }

        private record AttackResult(boolean performed, float damage) {}

        /**
         * @param passby If passby=true, and this isn't a complete absorb, the action will not perform. Else it will.
         * @return (Whether action had been really performed, processed damage)
         */
        private AttackResult handleAttack(DamageSource dmgSource, float dmg, boolean passby) {
            float reflectDamage = lerpf(0.6f, 1.2f, ctx.getSkillExp()) * dmg;
            if (!passby) { // Perform the action.
                if (!_isAttacking) {
                    _isAttacking = true;
                    try {
                        consumeDamage(dmg);
                        ctx.addSkillExp(dmg * 0.0004f);

                        Entity sourceEntity = dmgSource.getDirectEntity();
                        if (sourceEntity != null && sourceEntity != player) {
                            ctx.attack(sourceEntity, reflectDamage);

                            if (!SideUtils.isClient())
                                sendToClient(MSG_EFFECT, sourceEntity.position());
                        }
                    } finally {
                        _isAttacking = false;
                    }
                }

                return new AttackResult(true, dmg - reflectDamage);
            } else {
                return new AttackResult(reflectDamage >= 1, dmg - reflectDamage);
            }
        }

        private boolean consumeEntity(float difficulty) {
            return ctx.consume(0, difficulty * lerpf(300, 160, ctx.getSkillExp()));
        }

        private void consumeDamage(float damage) {
            ctx.consumeWithForce(0, lerpf(20, 15, ctx.getSkillExp()) * damage);
        }

        private boolean consumeNormal() {
            return ctx.consume(0, lerpf(15, 11, ctx.getSkillExp()));
        }

    }

    @Environment(EnvType.CLIENT)
    @RegClientContext(VecReflectionContext.class)
    public static class VecReflectionContextC extends ClientContext {

        private IActivateHandler activateHandler;
        private final WaveEffectUI ui = new WaveEffectUI(0.4f, 110, 1.6f);
        private AuxGui overlay;

        public VecReflectionContextC(Context par) {
            super(par);
        }

        @Listener(channel=MSG_MADEALIVE, side=Side.CLIENT)
        private void l_alive() {
            if (isLocal()) {
                activateHandler = ActivateHandlers.terminatesContext(parent);
                ClientRuntime.instance().addActivateHandler(activateHandler);
                overlay = new AuxGui() {
                    @Override
                    public void draw(ScaledResolution r) {
                        ui.onFrame(r.getScaledWidth(), r.getScaledHeight());
                    }
                };
                AuxGui.register(overlay);
            }
        }

        @Listener(channel=MSG_TERMINATED, side=Side.CLIENT)
        private void l_terminate() {
            if (isLocal()) {
                ClientRuntime.instance().removeActiveHandler(activateHandler);
                if (overlay != null) overlay.dispose();
            }
        }

        @Listener(channel=MSG_REFLECT_ENTITY, side=Side.CLIENT)
        private void c_reflectEntity(Entity ent) {
            if (ent == null) return;
            reflect(ent, player);
            reflectEffect(entityHeadPos(ent));
        }

        @Listener(channel=MSG_EFFECT, side=Side.CLIENT)
        private void reflectEffect(Vec3 point) {
            WaveEffect eff = new WaveEffect(world(), 2, 1.1);
            eff.setPos(point.x, point.y, point.z);
            eff.setYRot(player.getYHeadRot());
            eff.setXRot(player.getXRot());

            world().addFreshEntity(eff);

            playSound(point);
        }

        private void playSound(Vec3 pos) {
            ACSounds.playClient(world(), pos.x, pos.y, pos.z, "vecmanip.vec_reflection", SoundSource.AMBIENT, 0.5f, 1.0f);
        }

    }

}
