package cn.lambdalib2.registry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a static method taking exactly one {@link StateEvents.StateEvent} subclass, which is invoked when that stage
 *  of loading is reached. Higher priority runs first.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface StateEventCallback {
    int priority() default 0;
}
