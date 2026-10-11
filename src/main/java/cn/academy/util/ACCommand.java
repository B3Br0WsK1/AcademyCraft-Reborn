package cn.academy.util;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

/**
 * Base of the AcademyCraft commands. The 1.12 commands parsed a plain argument array; to keep their behaviour the
 *  Brigadier command takes the rest of the line as one greedy argument and splits it.
 * @author WeAthFolD
 */
public abstract class ACCommand {

    final String commandName;
    protected String localName;

    public ACCommand(String name) {
        localName = commandName = name;
    }

    public String locInvalid() {
        return "ac.command.invalid";
    }

    public String locSuccessful() {
        return "ac.command.successful";
    }

    public String locNotLearned() {
        return "ac.command.notlearned";
    }

    public String locNoPlayer() {
        return "ac.command.noplayer";
    }

    public String getName() {
        return commandName;
    }

    public String getUsage() {
        return getLoc("usage");
    }

    protected String getLoc(String s) {
        return "ac.command." + localName + "." + s;
    }

    /**
     * @return Whether the source may use this command (op level 2 by default).
     */
    protected boolean hasPermission(CommandSourceStack source) {
        return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    /**
     * Executes the command with the space separated arguments.
     */
    public abstract void execute(CommandSourceStack source, String[] args);

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(commandName)
            .requires(this::hasPermission)
            .executes(ctx -> run(ctx, ""))
            .then(Commands.argument("args", StringArgumentType.greedyString())
                .executes(ctx -> run(ctx, StringArgumentType.getString(ctx, "args")))));
    }

    private int run(CommandContext<CommandSourceStack> ctx, String args) {
        String trimmed = args.trim();
        execute(ctx.getSource(), trimmed.isEmpty() ? new String[0] : trimmed.split("\\s+"));
        return 1;
    }

    /**
     * Sends a (translatable) message to the command source.
     */
    public static void sendChat(CommandSourceStack source, String key, Object... pars) {
        source.sendSystemMessage(Component.translatable(key, pars));
    }

}
