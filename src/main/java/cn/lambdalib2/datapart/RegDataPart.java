package cn.lambdalib2.datapart;

import cn.lambdalib2.event.EventHub;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.util.ReflectionUtils;
import cn.lambdalib2.util.Side;
import net.minecraft.world.entity.Entity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.EnumSet;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RegDataPart {

    /**
     * @return The type that this DataPart applies on. Also applies for all subclasses.
     */
    Class<? extends Entity> value();

    /**
     * @return At what sides this DataPart should be constructed
     */
    Side[] side() default { Side.CLIENT, Side.SERVER };

}

class RegDataPartImpl {

    @SuppressWarnings("unchecked")
    @StateEventCallback(priority = 100)
    private static void init(StateEvents.PreInit ev) {
        ReflectionUtils.getClasses(RegDataPart.class).forEach(type -> {
            RegDataPart anno = type.getAnnotation(RegDataPart.class);
            Class<? extends Entity> regType = anno.value();
            EntityData.register(
                (Class) type,
                EnumSet.copyOf(Arrays.asList(anno.side())),
                regType::isAssignableFrom
            );
        });
        EntityData.bake();
        EventHub.BUS.register(EntityData.EventListener.instance);
    }

}
