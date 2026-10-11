package cn.academy.ability.vanilla.vecmanip.skill;

import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.ClientRuntime.ActivateHandlers;
import cn.academy.ability.context.ClientRuntime.IActivateHandler;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.ContextManager;
import cn.academy.ability.context.RegClientContext;
import cn.academy.ability.ctrl.KeyDelegates;
import cn.academy.ability.vanilla.vecmanip.client.effect.WaveEffect;
import cn.academy.ability.vanilla.vecmanip.client.effect.WaveEffectUI;
import cn.academy.ability.vanilla.vecmanip.skill.EntityAffection.Affected;
import cn.academy.client.sound.ACSounds;
import cn.lambdalib2.auxgui.AuxGui;
import cn.lambdalib2.event.EventHub;
import cn.lambdalib2.event.SubscribeEvent;
import cn.lambdalib2.event.mc.LivingEvents.LivingHurtEvent;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.ScaledResolution;
import cn.lambdalib2.util.Side;
import cn.lambdalib2.util.VecUtils;
import cn.lambdalib2.util.WorldUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static cn.lambdalib2.util.MathUtils.lerpf;

/**
 * Vector deviation. Ported from Scala.
 */
public class VecDeviation extends Skill {

    public static final VecDeviation INSTANCE = new VecDeviation();

    private VecDeviation() {
        super("vec_deviation", 2);
        EventHub.BUS.register(this);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        rt.addKey(keyID, KeyDelegates.contextActivate(this, VecDeviationContext.class, VecDeviationContext::new));
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent evt) {
        if (evt.getEntity() instanceof Player player) {
            Optional<VecDeviationContext> ctx = ContextManager.instance.find(VecDeviationContext.class)
                .filter(c -> c.player == player);
            if (ctx.isPresent() && evt.getAmount() <= 9999) {
                evt.setAmount(ctx.get().reduceDamage(evt.getAmount()));
            }
        }
    }

    public static final String MSG_STOP_ENTITY = "stop_ent";
    public static final String MSG_PLAY = "playsnd";

    private static Field explosionPowerField;

    static int explosionPower(LargeFireball fireball) {
        try {
            if (explosionPowerField == null) {
                explosionPowerField = LargeFireball.class.getDeclaredField("explosionPower");
                explosionPowerField.setAccessible(true);
            }
            return explosionPowerField.getInt(fireball);
        } catch (ReflectiveOperationException e) {
            return 1;
        }
    }

    static void setExplosionPower(LargeFireball fireball, int power) {
        try {
            explosionPower(fireball);
            explosionPowerField.setInt(fireball, power);
        } catch (ReflectiveOperationException | NullPointerException ignored) {
        }
    }

    public static class VecDeviationContext extends Context<VecDeviation> {

        private final Set<Entity> visited = Collections.newSetFromMap(new java.util.WeakHashMap<>());

        private final float tickConsumption = lerpf(13, 5, ctx.getSkillExp());
        private final float comsumption = lerpf(15, 12, ctx.getSkillExp());
        private final float overloadToKeep = lerpf(80, 50, ctx.getSkillExp());
        private float overloadKeep = 0f;

        public VecDeviationContext(Player p) {
            super(p, INSTANCE);
        }

        float reduceDamage(float dmg) {
            float consumption = Math.min(ctx.cpData.getCP(), lerpf(15, 12, ctx.getSkillExp()));

            ctx.consume(0, consumption);
            ctx.addSkillExp(dmg * 0.0006f);

            sendToClient(MSG_PLAY, player.position());

            return dmg * (1 - lerpf(0.4f, 0.9f, ctx.getSkillExp()));
        }

        @Listener(channel=MSG_MADEALIVE, side=Side.SERVER)
        private void s_madeAlive() {
            ctx.consume(overloadToKeep, 0);
            overloadKeep = ctx.cpData.getOverload();
        }

        @Listener(channel=MSG_TICK, side=Side.SERVER)
        private void s_tick() {
            if (!ctx.consume(0, tickConsumption)) terminate();
            if (ctx.cpData.getOverload() < overloadKeep) ctx.cpData.setOverload(overloadKeep);
            // Check the entities around player, and stop them by probablity
            double range = 5;

            List<Entity> entities = WorldUtils.getEntities(player, range, t -> true);
            entities.removeAll(visited);

            Level world = world();
            for (Entity entity : entities) {
                if (EntityAffection.isMarked(entity)) continue;
                if (!(EntityAffection.getAffectInfo(entity) instanceof Affected affected)) continue;
                // Process not-marked and affected entities
                float difficulty = affected.difficulty();
                ctx.consumeWithForce(0, comsumption);
                if (entity instanceof LargeFireball lfireball) {
                    lfireball.discard();
                    world.explode(null, lfireball.getX(), lfireball.getY(), lfireball.getZ(),
                        explosionPower(lfireball), true, Level.ExplosionInteraction.MOB);

                    ctx.addSkillExp(0.001f * difficulty);
                    sendToClient(MSG_STOP_ENTITY, lfireball);
                } else if (entity instanceof SmallFireball sfireball) {
                    sfireball.discard();

                    ctx.addSkillExp(0.001f * difficulty);
                    sendToClient(MSG_STOP_ENTITY, sfireball);
                } else {
                    if (entity instanceof AbstractArrow arrow) {
                        arrow.setBaseDamage(0);
                    }

                    entity.setDeltaMovement(Vec3.ZERO);
                    entity.needsSync = true;

                    ctx.addSkillExp(0.001f * difficulty);

                    sendToClient(MSG_STOP_ENTITY, entity);

                    EntityAffection.mark(entity);
                }
            }

            visited.addAll(entities);
        }

        @Listener(channel=MSG_TICK, side={Side.CLIENT, Side.SERVER})
        private void g_tick() {
            if (!isRemote() || isLocal()) {
                float normConsume = lerpf(5, 2.5f, ctx.getSkillExp());
                float normOverload = lerpf(0.5f, 0.2f, ctx.getSkillExp());
                ctx.consume(normOverload, normConsume);
            }
        }

    }

    @Environment(EnvType.CLIENT)
    @RegClientContext(VecDeviationContext.class)
    public static class VecDeviationContextC extends ClientContext {

        private IActivateHandler activateHandler;
        private final WaveEffectUI ui = new WaveEffectUI(0.2f, 100, 1.4f);
        private AuxGui overlay;

        public VecDeviationContextC(Context par) {
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

        @Listener(channel=MSG_STOP_ENTITY, side=Side.CLIENT)
        public void c_stopEntity(Entity ent) {
            if (ent == null) return;
            if (EntityAffection.isMarked(ent)) {
                WaveEffect eff = new WaveEffect(world(), 1, 0.6);
                Vec3 pos = VecUtils.entityHeadPos(ent);
                eff.setPos(pos.x, pos.y, pos.z);
                eff.setYRot(player.getYRot());
                eff.setXRot(player.getXRot());

                world().addFreshEntity(eff);
                playSound(eff.position());
            }

            ent.setDeltaMovement(Vec3.ZERO);
        }

        @Listener(channel=MSG_PLAY, side=Side.CLIENT)
        private void playSound(Vec3 pos) {
            ACSounds.playClient(world(), pos.x, pos.y, pos.z, "vecmanip.vec_deviation", SoundSource.AMBIENT, 0.5f, 1.0f);
        }

    }

}
