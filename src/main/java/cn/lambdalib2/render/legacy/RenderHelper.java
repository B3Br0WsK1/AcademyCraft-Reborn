package cn.lambdalib2.render.legacy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Stub of the 1.12 RenderHelper.
 */
@Environment(EnvType.CLIENT)
public final class RenderHelper {
    private RenderHelper() {}

    public static void enableStandardItemLighting() {}
    public static void disableStandardItemLighting() {}
    public static void enableGUIStandardItemLighting() {}
}
