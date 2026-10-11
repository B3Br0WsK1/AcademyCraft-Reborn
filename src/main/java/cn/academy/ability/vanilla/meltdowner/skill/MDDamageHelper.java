package cn.academy.ability.vanilla.meltdowner.skill;
import cn.academy.ability.AbilityContext;
import cn.academy.datapart.AbilityData;
import cn.academy.ability.vanilla.meltdowner.CatMeltdowner;
import cn.academy.client.render.particle.MdParticleFactory;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.NetworkS11nType;
import cn.lambdalib2.s11n.network.TargetPoints;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.VecUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import cn.lambdalib2.event.EventHub;
import cn.lambdalib2.event.mc.LivingEvents.LivingUpdateEvent;
import cn.lambdalib2.event.mc.LivingEvents.LivingHurtEvent;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.event.SubscribeEvent;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;

import cn.academy.ability.vanilla.meltdowner.passiveskill.RadiationIntensify;

/**
 * @author WeAthFolD
 */
@NetworkS11nType
public class MDDamageHelper {
    
    private static final String MARKID = "md_marktick", RATEID = "md_markrate";

    @StateEventCallback
    private static void init(StateEvents.Init event) {
        EventHub.BUS.register(new Events());
    }
    
    static void attack(AbilityContext ctx, Entity target, float dmg) {
        Player player = ctx.player;

        ctx.attack(target, dmg);
        AbilityData aData = AbilityData.get(player);
        if(aData.isSkillLearned(CatMeltdowner.radIntensify)) {
            int marktick = Math.max(60, getMarkTick(player));

            setMarkTick(target, marktick);
            setMarkRate(target, RadiationIntensify.INSTANCE.getRate(aData));
            NetworkMessage.sendToAllAround(
                    TargetPoints.convert(player, 20),
                    NetworkMessage.staticCaller(MDDamageHelper.class),
                    "sync", player, marktick
            );
        }
    }
    
    private static int getMarkTick(Entity player) {
        if(cn.lambdalib2.util.EntityTags.get(player).contains(MARKID))
            return cn.lambdalib2.util.EntityTags.get(player).getIntOr(MARKID, 0);
        else
            return 0;
    }

    private static float getMarkRate(Entity entity) {
        if(cn.lambdalib2.util.EntityTags.get(entity).contains(RATEID))
            return cn.lambdalib2.util.EntityTags.get(entity).getFloatOr(RATEID, 0);
        else
            return 0;
    }

    private static void setMarkRate(Entity entity, float amt) {
        cn.lambdalib2.util.EntityTags.get(entity).putFloat(RATEID, amt);
    }

    @Listener(channel="sync", side= Side.CLIENT)
    private static void setMarkTick(Entity player, int ticks) {
        cn.lambdalib2.util.EntityTags.get(player).putInt(MARKID, ticks);
    }
    
    public static class Events {
        
        @SubscribeEvent
        public void onLivingUpdate(LivingUpdateEvent event) {
            int tick = getMarkTick(event.getEntity());
            if(tick > 0)
                setMarkTick(event.getEntity(), tick - 1);
        }
        
        @Environment(EnvType.CLIENT)
        @SubscribeEvent
        public void onUpdateClient(LivingUpdateEvent event) {
            Entity e = event.getEntity();
            if(e.level().isClientSide()) {
                if(getMarkTick(e) > 0) {
                    int times = RandUtils.rangei(0, 3);
                    while(times --> 0) {
                        double r = RandUtils.ranged(.6, .7) * e.getBbWidth();
                        double theta = RandUtils.nextDouble() * 2 * Math.PI;
                        double h = RandUtils.ranged(0, e.getBbHeight());
                        
                        Vec3 pos = VecUtils.add(new Vec3(e.getX(), e.getY(), e.getZ()),
                            new Vec3(r * Math.sin(theta), h, r * Math.cos(theta)));
                        Vec3 vel = VecUtils.multiply(VecUtils.random(), 0.02);
                        e.level().addFreshEntity(MdParticleFactory.INSTANCE.next(e.level(), pos, vel));
                    }
                }
            }
        }
        
        @SubscribeEvent
        public void onLivingAttack(LivingHurtEvent event) {
            if(getMarkTick(event.getEntityLiving()) > 0) {
                event.setAmount(event.getAmount() * getMarkRate(event.getEntityLiving()));
            }
        }
        
    }
}