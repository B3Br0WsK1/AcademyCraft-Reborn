package cn.academy.command;
import cn.academy.advancements.ACAdvancements;
import cn.academy.util.ACCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.player.Player;


/**
 * @author EAirPeter
 */
public class CommandACACH extends ACCommand {

    public CommandACACH() {
        super("acach");
    }

    @Override
    public void execute(CommandSourceStack sender, String[] args) {
        // /acach ACHIEVEMENT_NAME [PLAYER_NAME]
        String nAch = null;
        String nPlayer = null;
        if (args.length > 0)
            nAch = args[0];
        if (nAch == null) {
            sendChat(sender, "Usage: /acach ACHIEVEMENT_NAME [PLAYER_NAME]");
            return;
        }
        if (args.length > 1)
            nPlayer = args[1];
        if (nPlayer == null)
            nPlayer = sender.getTextName();
        Player player = sender.getServer().getPlayerList().getPlayerByName(nPlayer);
        if (player == null) {
            sendChat(sender, locNoPlayer());
            return;
        }
        if (ACAdvancements.trigger(player, nAch))
            sendChat(sender, locSuccessful());
        else
            sendChat(sender, "No such achievement found");
    }
}
