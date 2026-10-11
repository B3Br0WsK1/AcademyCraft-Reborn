package cn.lambdalib2.mixin;

import cn.lambdalib2.event.EventHub;
import cn.lambdalib2.event.mc.PlayerEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@link PlayerEvents.ItemCraftedEvent}.
 */
@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin {

    @Shadow
    @Final
    private Player player;

    @Inject(method = "checkTakeAchievements", at = @At("HEAD"))
    private void lambdalib2$onCrafted(ItemStack stack, CallbackInfo ci) {
        if (!stack.isEmpty()) {
            EventHub.BUS.post(new PlayerEvents.ItemCraftedEvent(player, stack));
        }
    }

}
