package cn.academy.util;

import cn.lambdalib2.input.KeyHandler;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.util.ReflectionUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Registers a static KeyHandler field into {@link ACKeyManager} with a configurable key. The key id is a
 *  {@link cn.lambdalib2.input.Keys} id.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RegACKeyHandler {
    String name();
    int keyID();
    String comment() default "";
}

@Environment(EnvType.CLIENT)
class RegACKeyHandlerImple {
    @StateEventCallback
    private static void init(StateEvents.Init ev) {
        ReflectionUtils.getFields(RegACKeyHandler.class).forEach(field -> {
            RegACKeyHandler anno = field.getAnnotation(RegACKeyHandler.class);
            try {
                ACKeyManager.instance.addKeyHandler(anno.name(), anno.comment(), anno.keyID(), (KeyHandler) field.get(null));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
