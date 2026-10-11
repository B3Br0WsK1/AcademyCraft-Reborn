package cn.lambdalib2.event;

import java.lang.annotation.*;

/**
 * Marks an event handler method. The single parameter type decides which events it receives.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubscribeEvent {
    EventPriority priority() default EventPriority.NORMAL;
    boolean receiveCanceled() default false;
}
