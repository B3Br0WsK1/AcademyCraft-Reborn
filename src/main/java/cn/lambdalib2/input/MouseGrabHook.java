package cn.lambdalib2.input;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

/**
 * Allows a client feature to take over the mouse movement (replacement of the 1.12 MouseHelper swap). While a
 *  handler is set, mouse movement doesn't turn the player and is passed to the handler instead.
 */
@Environment(EnvType.CLIENT)
public final class MouseGrabHook {

    private MouseGrabHook() {}

    public interface Handler {
        /**
         * @param dx Raw mouse movement in x (pixels), positive to the right
         * @param dy Raw mouse movement in y (pixels), positive downwards
         */
        void onMouseMove(double dx, double dy);
    }

    private static @Nullable Handler handler;

    public static void set(@Nullable Handler h) {
        handler = h;
    }

    public static void clear(Handler h) {
        if (handler == h) {
            handler = null;
        }
    }

    public static @Nullable Handler get() {
        return handler;
    }

}
