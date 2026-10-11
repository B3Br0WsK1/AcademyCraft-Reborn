package cn.lambdalib2.mixin;

import cn.lambdalib2.event.EventHub;
import cn.lambdalib2.event.mc.PlayerEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@link PlayerEvents.ItemSmeltedEvent}.
 */
@Mixin(FurnaceResultSlot.class)
public abstract class FurnaceResultSlotMixin {

    @Shadow
    @Final
    private Player player;

    @Inject(method = "checkTakeAchievements", at = @At("HEAD"))
    private void lambdalib2$onSmelted(ItemStack stack, CallbackInfo ci) {
        if (!stack.isEmpty()) {
            EventHub.BUS.post(new PlayerEvents.ItemSmeltedEvent(player, stack));
        }
    }

}
