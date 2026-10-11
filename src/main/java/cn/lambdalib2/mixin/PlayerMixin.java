package cn.lambdalib2.mixin;

import cn.lambdalib2.event.EventHub;
import cn.lambdalib2.event.mc.EventHooks;
import cn.lambdalib2.event.mc.PlayerEvents;
import cn.lambdalib2.event.mc.TickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void lambdalib2$tickStart(CallbackInfo ci) {
        EventHub.BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.START, (Player) (Object) this));
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void lambdalib2$tickEnd(CallbackInfo ci) {
        EventHub.BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, (Player) (Object) this));
    }

    @ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true)
    private float lambdalib2$actuallyHurt(float dmg, ServerLevel level, DamageSource source) {
        return EventHooks.fireHurt((Player) (Object) this, source, dmg);
    }

    @Inject(method = "stopSleepInBed", at = @At("HEAD"))
    private void lambdalib2$wakeUp(boolean forceful, boolean updateLevelList, CallbackInfo ci) {
        EventHub.BUS.post(new PlayerEvents.PlayerWakeUpEvent((Player) (Object) this, forceful, updateLevelList));
    }
}
