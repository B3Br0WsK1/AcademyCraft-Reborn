package cn.lambdalib2.event.mc;

import cn.lambdalib2.event.EventHub;
import cn.lambdalib2.event.mc.LivingEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Shared logic of the event bridging mixins.
 */
public final class EventHooks {

    private EventHooks() {}

    public static float fireHurt(LivingEntity entity, DamageSource source, float dmg) {
        LivingEvents.LivingHurtEvent event = new LivingEvents.LivingHurtEvent(entity, source, dmg);
        if (EventHub.BUS.post(event)) {
            return 0.0f;
        }
        return Math.max(0.0f, event.getAmount());
    }
}
