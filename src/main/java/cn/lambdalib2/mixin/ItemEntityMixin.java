package cn.lambdalib2.mixin;

import cn.lambdalib2.event.EventHub;
import cn.lambdalib2.event.mc.PlayerEvents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@link PlayerEvents.ItemPickupEvent} once a player actually picked up (part of) the item.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Unique
    private ItemStack lambdalib2$touchStack;

    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void lambdalib2$beforeTouch(Player player, CallbackInfo ci) {
        lambdalib2$touchStack = ((ItemEntity) (Object) this).getItem().copy();
    }

    @Inject(method = "playerTouch", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/player/Player;take(Lnet/minecraft/world/entity/Entity;I)V"))
    private void lambdalib2$onPickup(Player player, CallbackInfo ci) {
        ItemStack stack = lambdalib2$touchStack;
        if (stack != null && !stack.isEmpty()) {
            EventHub.BUS.post(new PlayerEvents.ItemPickupEvent(player, (ItemEntity) (Object) this, stack));
        }
    }

}
