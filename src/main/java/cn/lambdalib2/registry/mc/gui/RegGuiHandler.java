package cn.lambdalib2.registry.mc.gui;

import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.util.ReflectionUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Registers a static {@link GuiHandlerBase} field. The menu type id is derived from the declaring class and field name.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RegGuiHandler {
}

class RegGuiHandlerImpl {

    static final List<GuiHandlerBase> handlers = new ArrayList<>();

    @StateEventCallback(priority = 50)
    private static void preInit(StateEvents.PreInit ev) {
        for (Field field : ReflectionUtils.getFields(RegGuiHandler.class)) {
            try {
                GuiHandlerBase handler = (GuiHandlerBase) field.get(null);
                Class<?> declaring = field.getDeclaringClass();
                String modid = declaring.getName().startsWith("cn.academy") ? "academy" : "lambdalib2";
                String path = (declaring.getSimpleName() + "_" + field.getName())
                    .replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
                handler.init(Identifier.fromNamespaceAndPath(modid, path));
                handlers.add(handler);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

}

@Environment(EnvType.CLIENT)
class RegGuiHandlerClientImpl {

    @StateEventCallback
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void init(StateEvents.Init ev) {
        for (GuiHandlerBase handler : RegGuiHandlerImpl.handlers) {
            MenuScreens.register(handler.getMenuType(),
                (MenuScreens.ScreenConstructor) (menu, inventory, title) -> {
                    Screen screen = handler.getClientScreen((AbstractContainerMenu) menu, ((net.minecraft.world.entity.player.Inventory) inventory).player);
                    return (Screen & MenuAccess) screen;
                });
        }
    }

}
