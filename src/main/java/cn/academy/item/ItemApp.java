package cn.academy.item;

import cn.academy.terminal.App;
import cn.academy.terminal.AppRegistry;
import cn.academy.terminal.TerminalData;
import cn.lambdalib2.util.Debug;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * ItemInstaller app
 * @author WeAthFolD
 */
public class ItemApp extends Item {

    private static final Map<String, ItemApp> items = new HashMap<>();

    public static ItemApp getItemForApp(App app) {
        return items.get(app.getName());
    }

    private final String _appName;

    private App _app;

    public ItemApp(String name, Properties props) { // Ctor is called during class construction, so we can't get app at that time.
        super(props);
        _appName = name;

        items.put(_appName, this);
    }

    private App getApp() {
        if (_app == null) {
            _app = Debug.assertNotNull(AppRegistry.getByName(_appName), () -> "App not found: " + _appName);
        }
        return _app;
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        App app = getApp();
        if(!world.isClientSide()) {
            TerminalData terminalData = TerminalData.get(player);
            if(!terminalData.isTerminalInstalled()) {
                player.sendSystemMessage(Component.translatable("ac.terminal.notinstalled"));
            } else if(terminalData.isInstalled(app)) {
                player.sendSystemMessage(
                    Component.translatable("ac.terminal.app_alrdy_installed", Component.translatable(app.getDisplayKey()))
                );
            } else {
                if(!player.getAbilities().instabuild)
                    stack.shrink(1);
                terminalData.installApp(app);
                player.sendSystemMessage(
                    Component.translatable("ac.terminal.app_installed", Component.translatable(app.getDisplayKey()))
                );
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable(getApp().getDisplayKey()));
    }

}
