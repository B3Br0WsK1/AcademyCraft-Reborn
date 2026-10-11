package cn.lambdalib2.input;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Raw keyboard/mouse input listeners active while no screen is open (replacement of the 1.12 InputEvent). A listener
 *  returning true consumes the input (vanilla won't handle it).
 */
@Environment(EnvType.CLIENT)
public final class RawInputHooks {

    private RawInputHooks() {}

    public interface Listener {
        /**
         * @param key The key (InputConstants key value)
         * @param press true if pressed (or repeated), false if released
         */
        default boolean onKey(int key, boolean press) { return false; }

        default boolean onChar(int codepoint) { return false; }

        /**
         * @param button The mouse button (0 left, 1 right, 2 middle)
         */
        default boolean onMouseButton(int button, boolean press) { return false; }

        /**
         * @param delta Vertical scroll amount (positive = up), in wheel notches
         */
        default boolean onScroll(double delta) { return false; }
    }

    private static final List<Listener> listeners = new CopyOnWriteArrayList<>();

    public static void add(Listener l) {
        listeners.add(l);
        updateTextInput();
    }

    /**
     * SDL only delivers character input while text input is started; keep it on while any listener is registered.
     */
    private static void updateTextInput() {
        Minecraft mc = Minecraft.getInstance();
        if (listeners.isEmpty()) {
            mc.textInputManager().stopTextInput(RawInputHooks.class);
        } else {
            mc.textInputManager().startTextInput(RawInputHooks.class);
        }
    }

    public static void remove(Listener l) {
        listeners.remove(l);
        updateTextInput();
    }

    private static boolean active() {
        return !listeners.isEmpty() && Minecraft.getInstance().gui.screen() == null;
    }

    public static boolean fireKey(int key, boolean press) {
        if (!active()) return false;
        boolean ret = false;
        for (Listener l : listeners) ret |= l.onKey(key, press);
        return ret;
    }

    public static boolean fireChar(int codepoint) {
        if (!active()) return false;
        boolean ret = false;
        for (Listener l : listeners) ret |= l.onChar(codepoint);
        return ret;
    }

    public static boolean fireMouseButton(int button, boolean press) {
        if (!active()) return false;
        boolean ret = false;
        for (Listener l : listeners) ret |= l.onMouseButton(button, press);
        return ret;
    }

    public static boolean fireScroll(double delta) {
        if (!active()) return false;
        boolean ret = false;
        for (Listener l : listeners) ret |= l.onScroll(delta);
        return ret;
    }

}
