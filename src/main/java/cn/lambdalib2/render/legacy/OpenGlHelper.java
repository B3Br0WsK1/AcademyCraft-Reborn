package cn.lambdalib2.render.legacy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Stub of the 1.12 OpenGlHelper. Lightmap coords are ignored by the emulation (everything is full bright).
 */
@Environment(EnvType.CLIENT)
public final class OpenGlHelper {
    private OpenGlHelper() {}

    public static final int lightmapTexUnit = 0x84C1, defaultTexUnit = 0x84C0;
    public static float lastBrightnessX = 240, lastBrightnessY = 240;

    public static void setLightmapTextureCoords(int target, float x, float y) {}

    public static void setActiveTexture(int texture) {}
}
