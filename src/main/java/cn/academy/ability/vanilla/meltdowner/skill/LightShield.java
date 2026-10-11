package cn.academy.ability.vanilla.meltdowner.skill;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.ContextManager;
import cn.academy.client.render.particle.MdParticleFactory;
import cn.academy.client.sound.ACSounds;
import cn.academy.client.sound.FollowEntitySound;
import cn.academy.entity.EntityMdShield;
import cn.lambdalib2.particle.Particle;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import cn.lambdalib2.event.EventHub;
import cn.lambdalib2.event.mc.LivingEvents.LivingHurtEvent;
import cn.lambdalib2.event.SubscribeEvent;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import static cn.lambdalib2.util.MathUtils.lerpf;
import static cn.lambdalib2.util.RandUtils.ranged;
import net.fabricmc.api.EnvType;




public class LightShield extends Skill
{
    public static final LightShield INSTANCE = new LightShield();
    private static final int ACTION_INTERVAL = 18;


    public LightShield()
    {
        super("light_shield", 2);
        EventHub.BUS.register(this);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void activate(ClientRuntime rt, int keyID)
    {
        activateSingleKey2(rt, keyID, LSContext::new);
    }

    @SubscribeEvent
    public void onPlayerAttacked(LivingHurtEvent event) {
        if(event.getEntityLiving() instanceof Player)
        {
            Optional<LSContext> context = ContextManager.instance.find(LSContext.class);
            if(context.isPresent())
            {
                event.setAmount(context.get().handleAttacked(event.getSource(), event.getAmount()));
                if (event.getAmount() == 0) 
                    event.setCanceled(true);
            }
        }
    }
    
    public static class LSContext extends Context
    {
        private Predicate<Entity> basicSelector = EntitySelectors.everything();
        private int ticks = 0;
        private int lastAbsorb = -1;// The tick last the shield absorbed damage.
        private float exp = ctx.getSkillExp();
        
        private final float MAX_TIME = lerpf(120, 180, exp);
        private int getCooldown(int ct){return (int)lerpf(2*ct, ct, exp);}
        private float overloadKeep = 0f;
        
        public LSContext(Player p)
        {
            super(p, LightShield.INSTANCE);
        }

        @Listener(channel=MSG_KEYUP, side=Side.CLIENT)
        private void l_onEnd()
        {
            terminate();
        }

        @Listener(channel=MSG_KEYABORT, side=Side.CLIENT) 
        private void l_onAbort()
        {
            terminate();
        }

        @Listener(channel=MSG_MADEALIVE, side=Side.SERVER) 
        private void s_madeAlive()
        {
            float overload = lerpf(110, 60, exp);
            ctx.consume(overload, 0);
            overloadKeep = ctx.cpData.getOverload();
        }

        @Listener(channel=MSG_TICK, side=Side.SERVER) 
        private void s_tick()
        {
        if(ctx.cpData.getOverload() < overloadKeep)
            ctx.cpData.setOverload(overloadKeep);
        ticks += 1;
        if(ticks > MAX_TIME)
            terminate();

        float cp = lerpf(9, 4, exp);
        if (!ctx.consume(0, cp))
            terminate();
        ctx.addSkillExp(1e-6f);

        // Find the entities that are 'colliding' with the shield.
        List<Entity> candidates = WorldUtils.getEntities(player, 3, basicSelector.and(t -> isEntityReachable(t)).and(EntitySelectors.exclude(player)));
        for (Entity e : candidates) {
            if (e.getInvulnerableTime() <= 0 && ctx.consume(getAbsorbOverload(), getAbsorbConsumption()))
            {
                MDDamageHelper.attack(ctx, e, getTouchDamage());
                ctx.addSkillExp(.001f);
            }
        }
    }

        @Listener(channel=MSG_TERMINATED, side=Side.SERVER) 
        private void s_onEnd(){ 

        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1));
        ctx.setCooldown(getCooldown(ticks));
    }

        float handleAttacked(DamageSource src, float damage)
        {
            float result = damage;
            if (damage == 0 || lastAbsorb != -1 && ticks - lastAbsorb <= ACTION_INTERVAL)
                return damage;
            Entity entity  = src.getDirectEntity();
            boolean perform = false;
            if (entity != null)
            {
                if (isEntityReachable(entity))
                    perform = true;
            }
            else
                perform = true;

            if (perform)
            {
                lastAbsorb = ticks;
                if (ctx.consume(getAbsorbConsumption(), getAbsorbOverload()))
                {
                    float amt = getAbsorbDamage();
                    result -= Math.min(damage, amt);
                }
            }
            ctx.addSkillExp(.001f);
            return result;
        }

        private float getAbsorbDamage(){return lerpf(15, 50, exp);}

        private float getTouchDamage(){return lerpf(2, 6, exp);}

        private float getAbsorbOverload(){return lerpf(5, 3, exp);}

        private float getAbsorbConsumption(){return lerpf(50, 30, exp);}

        private boolean isEntityReachable(Entity e)
        {
            double dx = e.getX() - player.getX();
            //dy = e.getY() - player.getY(),
            double dz = e.getZ() - player.getZ();
            double yaw = -MathUtils.toDegrees(Math.atan2(dx, dz));
            return Math.abs(yaw - player.getYRot()) % 360 < 60;
        }

        @Environment(EnvType.CLIENT)
        private EntityMdShield shield;
        @Environment(EnvType.CLIENT)
        private FollowEntitySound loopSound;

        @Environment(EnvType.CLIENT)
        @Listener(channel=MSG_MADEALIVE, side=Side.CLIENT)
        private void c_spawn()
        {
            shield = new EntityMdShield(player);
            world().addFreshEntity(shield);
            ACSounds.playClient(player, "md.shield_startup", SoundSource.AMBIENT, 0.5f);
            loopSound = new FollowEntitySound(player, "md.shield_loop", SoundSource.AMBIENT).setLoop();
            ACSounds.playClient(loopSound);
        }

        @Environment(EnvType.CLIENT)
        @Listener(channel=MSG_TICK, side=Side.CLIENT)
        private void c_update()
        {
            if (RandUtils.nextFloat() < 0.3f) {
                double s = 0.5;
                Vec3 mo = VecUtils.lookingPos(player, 1).add(
                        ranged(-s, s), ranged(-s, s), ranged(-s, s)
                );
                Particle p = MdParticleFactory.INSTANCE.next(world(), new Vec3(mo.x, mo.y, mo.z),
                        new Vec3(ranged(-.02, .02), ranged(-.01, .05), ranged(-.02, .02)));
                world().addFreshEntity(p);
            }
        }

        @Environment(EnvType.CLIENT)
        @Listener(channel=MSG_TERMINATED, side=Side.CLIENT)
        private void c_end(){
            shield.discard();
            loopSound.stop();
        }
    }
}
