package cn.lambdalib2.input;

import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.util.ReflectionUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RegDynamicKeyHandler {
    int keyID();
}

@Environment(EnvType.CLIENT)
class RegDynKeyHandlerImpl {

    @StateEventCallback
    private static void init(StateEvents.Init ev) {
        ReflectionUtils.getFields(RegDynamicKeyHandler.class)
            .forEach(field -> {
                RegDynamicKeyHandler anno = field.getAnnotation(RegDynamicKeyHandler.class);
                try {
                    KeyManager.dynamic.addKeyHandler(anno.keyID(), (KeyHandler) field.get(null));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            });
    }

}
