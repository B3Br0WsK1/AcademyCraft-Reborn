package cn.lambdalib2.s11n;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SerializeStrategy {

    enum ExposeStrategy {
        /** Just public fields */
        PUBLIC,
        /** Includes private fields */
        ALL
    }

    ExposeStrategy strategy() default ExposeStrategy.PUBLIC;

    /**
     * @return Should we serialize all the fields exposed in the class, not just serializable ones.
     */
    boolean all() default false;

}
