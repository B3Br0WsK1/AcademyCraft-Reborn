package cn.lambdalib2.mixin;

import cn.lambdalib2.datapart.EntityData;
import cn.lambdalib2.event.EventHub;
import cn.lambdalib2.event.mc.EventHooks;
import cn.lambdalib2.event.mc.LivingEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void lambdalib2$tick(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (EventHub.BUS.post(new LivingEvents.LivingUpdateEvent(self))) {
            ci.cancel();
            return;
        }
        EntityData._onEntityTick(self);
    }

    @ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true)
    private float lambdalib2$actuallyHurt(float dmg, ServerLevel level, DamageSource source) {
        return EventHooks.fireHurt((LivingEntity) (Object) this, source, dmg);
    }
}
