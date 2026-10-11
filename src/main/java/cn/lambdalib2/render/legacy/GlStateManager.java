package cn.lambdalib2.render.legacy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Stub of the 1.12 GlStateManager, routed to the {@link LegacyGL} emulation.
 */
@Environment(EnvType.CLIENT)
public final class GlStateManager {
    private GlStateManager() {}

    public static void pushMatrix() { LegacyGL.pushMatrix(); }
    public static void popMatrix() { LegacyGL.popMatrix(); }
    public static void translate(double x, double y, double z) { LegacyGL.translate(x, y, z); }
    public static void scale(double x, double y, double z) { LegacyGL.scale(x, y, z); }
    public static void rotate(float a, float x, float y, float z) { LegacyGL.rotate(a, x, y, z); }
    public static void color(float r, float g, float b, float a) { LegacyGL.color(r, g, b, a); }
    public static void color(float r, float g, float b) { LegacyGL.color(r, g, b, 1); }
    public static void enableBlend() { LegacyGL.enable(GL11.GL_BLEND); }
    public static void disableBlend() { LegacyGL.disable(GL11.GL_BLEND); }
    public static void enableTexture2D() { LegacyGL.enable(GL11.GL_TEXTURE_2D); }
    public static void disableTexture2D() { LegacyGL.disable(GL11.GL_TEXTURE_2D); }
    public static void enableDepth() { LegacyGL.enable(GL11.GL_DEPTH_TEST); }
    public static void disableDepth() { LegacyGL.disable(GL11.GL_DEPTH_TEST); }
    public static void depthMask(boolean b) { LegacyGL.depthMask(b); }
    public static void blendFunc(int s, int d) { LegacyGL.blendFunc(s, d); }
    public static void enableLighting() {}
    public static void disableLighting() {}
    public static void enableCull() {}
    public static void disableCull() {}
    public static void enableAlpha() {}
    public static void disableAlpha() {}
    public static void bindTexture(int id) {}
}
