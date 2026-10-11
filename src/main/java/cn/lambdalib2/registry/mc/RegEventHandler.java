package cn.lambdalib2.registry.mc;

import cn.lambdalib2.event.EventHub;
import cn.lambdalib2.registry.AnnotationScanner;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.util.Debug;
import cn.lambdalib2.util.ReflectionUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

/**
 * Registers the static field's value as a listener on {@link Events#BUS}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RegEventHandler {
}

class RegEventHandlerImpl {

    static void registerAll(boolean clientOnly) {
        for (AnnotationScanner.Entry e : ReflectionUtils.getRawObjects(RegEventHandler.class)) {
            if (AnnotationScanner.isClientOnlyEntry(e) != clientOnly) continue;
            Field field = ReflectionUtils.toField(e);
            try {
                Object obj = field.get(null);
                Debug.assertNotNull(obj, () -> "RegEventHandler field is null: " + field);
                EventHub.BUS.register(obj);
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @StateEventCallback
    private static void preInit(StateEvents.PreInit ev) {
        registerAll(false);
    }

}

@Environment(EnvType.CLIENT)
class RegEventHandlerClientImpl {

    @StateEventCallback
    private static void preInit(StateEvents.PreInit ev) {
        RegEventHandlerImpl.registerAll(true);
    }

}
