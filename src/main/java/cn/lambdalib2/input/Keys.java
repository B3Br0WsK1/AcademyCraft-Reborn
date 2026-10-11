package cn.lambdalib2.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

/**
 * Key id helpers. Keyboard keys use the game's native key codes ({@link InputConstants}, SDL scancodes in 26.x);
 *  mouse buttons use the negative ids of the original LambdaLib ({@link #MOUSE_LEFT} etc.).
 *  The {@code KEY_*} constants mirror the names of LWJGL2's Keyboard constants used by the original code.
 */
public final class Keys {

    public static final int
        MOUSE_LEFT = -100, MOUSE_RIGHT = -99, MOUSE_MIDDLE = -98,
        MWHEELDOWN = -50, MWHEELUP = -49;

    public static final int
        KEY_NONE = 0,
        KEY_A = InputConstants.KEY_A, KEY_B = InputConstants.KEY_B, KEY_C = InputConstants.KEY_C,
        KEY_D = InputConstants.KEY_D, KEY_E = InputConstants.KEY_E, KEY_F = InputConstants.KEY_F,
        KEY_G = InputConstants.KEY_G, KEY_H = InputConstants.KEY_H, KEY_I = InputConstants.KEY_I,
        KEY_J = InputConstants.KEY_J, KEY_K = InputConstants.KEY_K, KEY_L = InputConstants.KEY_L,
        KEY_M = InputConstants.KEY_M, KEY_N = InputConstants.KEY_N, KEY_O = InputConstants.KEY_O,
        KEY_P = InputConstants.KEY_P, KEY_Q = InputConstants.KEY_Q, KEY_R = InputConstants.KEY_R,
        KEY_S = InputConstants.KEY_S, KEY_T = InputConstants.KEY_T, KEY_U = InputConstants.KEY_U,
        KEY_V = InputConstants.KEY_V, KEY_W = InputConstants.KEY_W, KEY_X = InputConstants.KEY_X,
        KEY_Y = InputConstants.KEY_Y, KEY_Z = InputConstants.KEY_Z,
        KEY_0 = InputConstants.KEY_0, KEY_1 = InputConstants.KEY_1, KEY_2 = InputConstants.KEY_2,
        KEY_3 = InputConstants.KEY_3, KEY_4 = InputConstants.KEY_4, KEY_5 = InputConstants.KEY_5,
        KEY_6 = InputConstants.KEY_6, KEY_7 = InputConstants.KEY_7, KEY_8 = InputConstants.KEY_8,
        KEY_9 = InputConstants.KEY_9,
        KEY_F1 = InputConstants.KEY_F1, KEY_F2 = InputConstants.KEY_F2, KEY_F3 = InputConstants.KEY_F3,
        KEY_F4 = InputConstants.KEY_F4, KEY_F5 = InputConstants.KEY_F5, KEY_F6 = InputConstants.KEY_F6,
        KEY_RETURN = InputConstants.KEY_RETURN,
        KEY_NUMPADENTER = InputConstants.KEY_NUMPADENTER,
        KEY_BACK = InputConstants.KEY_BACKSPACE,
        KEY_ESCAPE = InputConstants.KEY_ESCAPE,
        KEY_TAB = InputConstants.KEY_TAB,
        KEY_SPACE = InputConstants.KEY_SPACE,
        KEY_LMENU = InputConstants.KEY_LALT,
        KEY_RMENU = InputConstants.KEY_RALT,
        KEY_LSHIFT = InputConstants.KEY_LSHIFT,
        KEY_RSHIFT = InputConstants.KEY_RSHIFT,
        KEY_LCONTROL = InputConstants.KEY_LCONTROL,
        KEY_RCONTROL = InputConstants.KEY_RCONTROL,
        KEY_UP = InputConstants.KEY_UP,
        KEY_DOWN = InputConstants.KEY_DOWN,
        KEY_LEFT = InputConstants.KEY_LEFT,
        KEY_RIGHT = InputConstants.KEY_RIGHT,
        KEY_DELETE = InputConstants.KEY_DELETE;

    private Keys() {}

    public static boolean isMouse(int keyId) {
        return keyId < 0;
    }

    /**
     * @return The vanilla key object corresponding to our key id.
     */
    @Environment(EnvType.CLIENT)
    public static InputConstants.Key toInputKey(int keyId) {
        if (keyId >= 0) {
            return InputConstants.Type.KEYBOARD.getOrCreate(keyId);
        }
        return InputConstants.Type.MOUSE.getOrCreate(toVanillaMouseButton(keyId));
    }

    /**
     * @return Our key id of the key currently bound to the given key mapping.
     */
    @Environment(EnvType.CLIENT)
    public static int fromKeyMapping(net.minecraft.client.KeyMapping mapping) {
        InputConstants.Key key = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf(mapping);
        if (key.getType() == InputConstants.Type.MOUSE) {
            return fromVanillaMouseButton(key.getValue());
        }
        return key.getValue();
    }

    /**
     * Converts a vanilla mouse button value to our negative id.
     */
    public static int fromVanillaMouseButton(int button) {
        switch (button) {
            case InputConstants.MOUSE_BUTTON_LEFT: return MOUSE_LEFT;
            case InputConstants.MOUSE_BUTTON_RIGHT: return MOUSE_RIGHT;
            case InputConstants.MOUSE_BUTTON_MIDDLE: return MOUSE_MIDDLE;
            default: return -100 + (button - 1);
        }
    }

    public static int toVanillaMouseButton(int keyId) {
        switch (keyId) {
            case MOUSE_LEFT: return InputConstants.MOUSE_BUTTON_LEFT;
            case MOUSE_RIGHT: return InputConstants.MOUSE_BUTTON_RIGHT;
            case MOUSE_MIDDLE: return InputConstants.MOUSE_BUTTON_MIDDLE;
            default: return keyId + 100 + 1;
        }
    }

    @Environment(EnvType.CLIENT)
    public static String getKeyName(int keyid) {
        if (keyid == KEY_NONE) {
            return "undefined";
        }
        try {
            return toInputKey(keyid).getDisplayName().getString();
        } catch (RuntimeException e) {
            return "undefined";
        }
    }

    /**
     * Key ids that are reported as pressed regardless of the real input state (used by automated tests).
     */
    public static final java.util.Set<Integer> forcedDown = java.util.concurrent.ConcurrentHashMap.newKeySet();

    @Environment(EnvType.CLIENT)
    public static boolean isKeyDown(int keyID) {
        if (!forcedDown.isEmpty() && forcedDown.contains(keyID)) {
            return true;
        }
        if (keyID > 0) {
            try {
                return InputConstants.isKeyDown(keyID);
            } catch (IndexOutOfBoundsException e) { // Unknown key code
                return false;
            }
        }
        Minecraft mc = Minecraft.getInstance();
        switch (keyID) {
            case MOUSE_LEFT: return mc.mouseHandler.isLeftPressed();
            case MOUSE_RIGHT: return mc.mouseHandler.isRightPressed();
            case MOUSE_MIDDLE: return mc.mouseHandler.isMiddlePressed();
            default: return false;
        }
    }
}
