package cn.academy.command;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/**
 * Registers the AcademyCraft commands.
 */
public final class ACCommands {

    private ACCommands() {}

    @StateEventCallback
    private static void init(StateEvents.Init ev) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            new CommandAIMBase.CommandAIM().register(dispatcher);
            new CommandAIMBase.CommandAIMP().register(dispatcher);
            new CommandACACH().register(dispatcher);
        });
    }

}
