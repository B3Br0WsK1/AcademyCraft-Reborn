package cn.academy.util;

import cn.academy.AcademyCraft;
import cn.lambdalib2.event.EventPriority;
import cn.lambdalib2.event.EventHub;
import cn.lambdalib2.event.SubscribeEvent;
import cn.lambdalib2.event.mc.LivingEvents.LivingHurtEvent;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Utils to help us debug.
 * @author WeAthFolD
 */
public class DebugHelper {

    @StateEventCallback
    private static void init(StateEvents.Init event) {
        if(AcademyCraft.DEBUG_MODE) {
            EventHub.BUS.register(new DebugHelper());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingAttack(LivingHurtEvent event) {
        if(event.getSource().getDirectEntity() instanceof Player player) {
            player.sendSystemMessage(Component.literal(
                String.format("%s: %.1f | %.1f/%.1f", event.getEntity().getClass().getSimpleName(), event.getAmount(),
                    event.getEntityLiving().getHealth(), event.getEntityLiving().getMaxHealth())));
        }
    }

}
