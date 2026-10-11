package cn.lambdalib2.event.mc;

import cn.lambdalib2.event.Cancelable;
import cn.lambdalib2.event.Event;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Events concerning living entities. Counterpart of Forge's LivingEvent family.
 */
public final class LivingEvents {

    private LivingEvents() {}

    public static class LivingEvent extends Event {
        private final LivingEntity entity;

        public LivingEvent(LivingEntity entity) {
            this.entity = entity;
        }

        public LivingEntity getEntityLiving() {
            return entity;
        }

        public LivingEntity getEntity() {
            return entity;
        }
    }

    /**
     * Fired at the beginning of {@link LivingEntity#tick()} on both sides.
     */
    @Cancelable
    public static class LivingUpdateEvent extends LivingEvent {
        public LivingUpdateEvent(LivingEntity e) {
            super(e);
        }
    }

    /**
     * Fired (server only) when an entity is about to be attacked. Canceling prevents the damage entirely.
     */
    @Cancelable
    public static class LivingAttackEvent extends LivingEvent {
        private final DamageSource source;
        private final float amount;

        public LivingAttackEvent(LivingEntity entity, DamageSource source, float amount) {
            super(entity);
            this.source = source;
            this.amount = amount;
        }

        public DamageSource getSource() {
            return source;
        }

        public float getAmount() {
            return amount;
        }
    }

    /**
     * Fired (server only) right before damage is applied. Amount can be modified; canceling negates the damage.
     */
    @Cancelable
    public static class LivingHurtEvent extends LivingEvent {
        private final DamageSource source;
        private float amount;

        public LivingHurtEvent(LivingEntity entity, DamageSource source, float amount) {
            super(entity);
            this.source = source;
            this.amount = amount;
        }

        public DamageSource getSource() {
            return source;
        }

        public float getAmount() {
            return amount;
        }

        public void setAmount(float amount) {
            this.amount = amount;
        }
    }

    /**
     * Fired (server only) when an entity dies. Canceling prevents the death.
     */
    @Cancelable
    public static class LivingDeathEvent extends LivingEvent {
        private final DamageSource source;

        public LivingDeathEvent(LivingEntity entity, DamageSource source) {
            super(entity);
            this.source = source;
        }

        public DamageSource getSource() {
            return source;
        }
    }
}
