package cn.academy.event;
import cn.academy.tutorial.ACTutorial;
import net.minecraft.world.entity.player.Player;
import cn.lambdalib2.event.mc.PlayerEvents.PlayerEvent;


/**
 * Fired at both client and server when player has activated a new tutorial.
 */
public class TutorialActivatedEvent extends PlayerEvent {

    public final ACTutorial tutorial;

    public TutorialActivatedEvent(Player player, ACTutorial tutorial) {
        super(player);
        this.tutorial = tutorial;
    }

}