package cn.lambdalib2.s11n.network;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a static NetS11nAdaptor field to be registered for the given type at startup.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RegNetS11nAdapter {
    Class value();
}
