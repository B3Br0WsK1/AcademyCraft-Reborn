package cn.lambdalib2.render.gui;

import cn.lambdalib2.mixin.client.GuiGraphicsExtractorAccessor;
import cn.lambdalib2.render.LLPipelines;
import cn.lambdalib2.util.Color;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Immediate-mode style 2D drawing on top of the deferred GUI renderer. Keeps a GL-like state (current color,
 *  bound texture, blend mode) so that code written against the old GL pipeline (HudUtils, CGui components) can be
 *  ported with minimal changes. Must be used between {@link #begin(GuiGraphicsExtractor)} and {@link #end()}.
 */
@Environment(EnvType.CLIENT)
public final class GuiDraw {

    public enum Blend { NORMAL, ADDITIVE }

    private static GuiGraphicsExtractor graphics;
    private static final Deque<GuiGraphicsExtractor> graphicsStack = new ArrayDeque<>();

    private static float cr = 1, cg = 1, cb = 1, ca = 1;
    private static Identifier texture;
    private static boolean texEnabled = false;
    private static Blend blend = Blend.NORMAL;
    private static boolean linearFilter = false;
    private static final java.util.Set<Identifier> linearTextures = java.util.concurrent.ConcurrentHashMap.newKeySet();

    // Quad builder
    private static float[] buf = new float[256];
    private static int[] colBuf = new int[64];
    private static int vcount = 0;
    private static boolean building = false;
    private static boolean buildTextured = false;

    private GuiDraw() {}

    public static void begin(GuiGraphicsExtractor g) {
        if (graphics != null) {
            graphicsStack.push(graphics);
        }
        graphics = g;
        resetState();
    }

    public static void end() {
        graphics = graphicsStack.isEmpty() ? null : graphicsStack.pop();
    }

    public static boolean isActive() {
        return graphics != null;
    }

    public static GuiGraphicsExtractor graphics() {
        if (graphics == null) {
            throw new IllegalStateException("GuiDraw used outside of a GUI draw context");
        }
        return graphics;
    }

    public static void resetState() {
        cr = cg = cb = ca = 1;
        texture = null;
        texEnabled = false;
        blend = Blend.NORMAL;
        linearFilter = false;
    }

    // ---- Color

    public static void color4f(float r, float g, float b, float a) {
        cr = r; cg = g; cb = b; ca = a;
    }

    public static void color4d(double r, double g, double b, double a) {
        color4f((float) r, (float) g, (float) b, (float) a);
    }

    public static void color3f(float r, float g, float b) {
        color4f(r, g, b, 1);
    }

    public static void color(Color c) {
        color4f(c.getRedF(), c.getGreenF(), c.getBlueF(), c.getAlphaF());
    }

    public static void colorARGB(int argb) {
        color4f(((argb >> 16) & 0xFF) / 255f, ((argb >> 8) & 0xFF) / 255f, (argb & 0xFF) / 255f, ((argb >>> 24) & 0xFF) / 255f);
    }

    public static void resetColor() {
        color4f(1, 1, 1, 1);
    }

    public static float getAlpha() {
        return ca;
    }

    public static int currentARGB() {
        return packARGB(cr, cg, cb, ca);
    }

    public static int packARGB(float r, float g, float b, float a) {
        int ir = clamp255(r), ig = clamp255(g), ib = clamp255(b), ia = clamp255(a);
        return (ia << 24) | (ir << 16) | (ig << 8) | ib;
    }

    private static int clamp255(float f) {
        int v = (int) (f * 255 + 0.5f);
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    // ---- Texture & blend

    /**
     * Binds a texture and enables texturing (like glBindTexture + glEnable(GL_TEXTURE_2D)).
     */
    public static void bindTexture(Identifier tex) {
        texture = tex;
        texEnabled = tex != null;
    }

    public static void enableTexture() {
        texEnabled = texture != null;
    }

    public static void disableTexture() {
        texEnabled = false;
    }

    public static boolean isTextureEnabled() {
        return texEnabled;
    }

    public static Identifier getTexture() {
        return texture;
    }

    public static void setBlend(Blend b) {
        blend = b;
    }

    public static Blend getBlend() {
        return blend;
    }

    public static void setLinearFilter(boolean linear) {
        linearFilter = linear;
    }

    /**
     * Marks a texture to be always sampled with linear filtering (used for large, smooth GUI textures).
     */
    public static void markLinear(Identifier texture) {
        linearTextures.add(texture);
    }

    // ---- Matrix

    public static Matrix3x2fStack pose() {
        return graphics().pose();
    }

    public static void pushMatrix() {
        pose().pushMatrix();
    }

    public static void popMatrix() {
        pose().popMatrix();
    }

    public static void translate(double x, double y) {
        pose().translate((float) x, (float) y);
    }

    public static void scale(double sx, double sy) {
        pose().scale((float) sx, (float) sy);
    }

    /**
     * Rotate around the Z axis, in degrees (like glRotate(angle, 0, 0, 1)).
     */
    public static void rotate(double degrees) {
        pose().rotate((float) Math.toRadians(degrees));
    }

    public static void enableScissor(int x0, int y0, int x1, int y1) {
        graphics().enableScissor(x0, y0, x1, y1);
    }

    public static void disableScissor() {
        graphics().disableScissor();
    }

    // ---- Quad building

    /**
     * Starts building quads with the current state. Vertices are added with {@link #vertex(double, double, double, double)}.
     */
    public static void beginQuads() {
        building = true;
        buildTextured = texEnabled;
        vcount = 0;
    }

    public static void vertex(double x, double y) {
        vertex(x, y, 0, 0, currentARGB());
    }

    public static void vertex(double x, double y, double u, double v) {
        vertex(x, y, u, v, currentARGB());
    }

    public static void vertex(double x, double y, double u, double v, int argb) {
        if (!building) throw new IllegalStateException("Not building");
        ensureCapacity(vcount + 1);
        int i = vcount * 4;
        buf[i] = (float) x;
        buf[i + 1] = (float) y;
        buf[i + 2] = (float) u;
        buf[i + 3] = (float) v;
        colBuf[vcount] = argb;
        ++vcount;
    }

    private static void ensureCapacity(int n) {
        if (n * 4 > buf.length) {
            float[] nb = new float[Math.max(buf.length * 2, n * 4)];
            System.arraycopy(buf, 0, nb, 0, vcount * 4);
            buf = nb;
        }
        if (n > colBuf.length) {
            int[] nc = new int[Math.max(colBuf.length * 2, n)];
            System.arraycopy(colBuf, 0, nc, 0, vcount);
            colBuf = nc;
        }
    }

    /**
     * Submits the quads built since {@link #beginQuads()}.
     */
    public static void endQuads() {
        building = false;
        int count = vcount - (vcount % 4);
        if (count == 0 || graphics == null) return;

        float[] data = new float[count * 4];
        System.arraycopy(buf, 0, data, 0, count * 4);
        int[] cols = new int[count];
        System.arraycopy(colBuf, 0, cols, 0, count);

        RenderPipeline pipeline;
        TextureSetup setup;
        if (buildTextured && texture != null) {
            pipeline = blend == Blend.ADDITIVE ? LLPipelines.GUI_TEXTURED_ADDITIVE : LLPipelines.GUI_TEXTURED;
            AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(texture);
            GpuSampler sampler = (linearFilter || linearTextures.contains(texture)) ?
                RenderSystem.getSamplerCache().getSampler(AddressMode.REPEAT, AddressMode.REPEAT, FilterMode.LINEAR, FilterMode.LINEAR, false) :
                tex.getSampler();
            setup = TextureSetup.singleTexture(tex.getTextureView(), sampler);
        } else {
            pipeline = blend == Blend.ADDITIVE ? LLPipelines.GUI_ADDITIVE : LLPipelines.GUI;
            setup = TextureSetup.noTexture();
        }

        GuiGraphicsExtractor g = graphics;
        ((GuiGraphicsExtractorAccessor) g).lambdalib2$getGuiRenderState().addGuiElement(
            new QuadsRenderState(pipeline, setup, buildTextured && texture != null, new Matrix3x2f(g.pose()),
                data, cols, count, g.scissorStack.peek()));
    }

    /**
     * Converts a triangle fan (center + ring) into quads by duplicating vertices.
     */
    public static void fanAsQuads(double cx, double cy, double[] xs, double[] ys, double[] us, double[] vs, double cu, double cv) {
        beginQuads();
        for (int i = 0; i + 1 < xs.length; ++i) {
            vertex(cx, cy, cu, cv);
            vertex(xs[i], ys[i], us == null ? 0 : us[i], vs == null ? 0 : vs[i]);
            vertex(xs[i + 1], ys[i + 1], us == null ? 0 : us[i + 1], vs == null ? 0 : vs[i + 1]);
            vertex(xs[i + 1], ys[i + 1], us == null ? 0 : us[i + 1], vs == null ? 0 : vs[i + 1]);
        }
        endQuads();
    }

    // ---- High level primitives (HudUtils equivalents)

    /**
     * Draws a rect. If texture is enabled, the whole texture (uv 0~1) is mapped onto it.
     */
    public static void rect(double x, double y, double width, double height) {
        rawRect(x, y, 0, 0, width, height, 1, 1);
    }

    /**
     * Draws a rect, with uv given in normalized (0~1) coordinates.
     */
    public static void rawRect(double x, double y, double u, double v, double width, double height, double texWidth, double texHeight) {
        beginQuads();
        vertex(x, y + height, u, v + texHeight);
        vertex(x + width, y + height, u + texWidth, v + texHeight);
        vertex(x + width, y, u + texWidth, v);
        vertex(x, y, u, v);
        endQuads();
    }

    /**
     * Draws a rect, with uv given in texture pixel coordinates.
     */
    public static void rect(double x, double y, double u, double v, double width, double height, double texWidth, double texHeight) {
        double tw = 1, th = 1;
        if (texture != null) {
            AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(texture);
            tw = tex.getTexture().getWidth(0);
            th = tex.getTexture().getHeight(0);
        }
        rawRect(x, y, u / tw, v / th, width, height, texWidth / tw, texHeight / th);
    }

    /**
     * Draws a rect with current color and no texture.
     */
    public static void colorRect(double x, double y, double width, double height) {
        boolean prev = texEnabled;
        texEnabled = false;
        rect(x, y, width, height);
        texEnabled = prev;
    }

    public static void drawRectOutline(double x, double y, double w, double h, float lineWidth) {
        double lw = lineWidth * 0.2;
        x -= lw;
        y -= lw;
        w += 2 * lw;
        h += 2 * lw;
        double t = Math.max(0.5, lineWidth * 0.5);
        colorRect(x, y, w, t);
        colorRect(x, y + h - t, w, t);
        colorRect(x, y, t, h);
        colorRect(x + w - t, y, t, h);
    }

    public static void line(double x0, double y0, double x1, double y1, double width) {
        double dx = x1 - x0, dy = y1 - y0;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-6) return;
        double nx = -dy / len * width / 2, ny = dx / len * width / 2;
        boolean prev = texEnabled;
        texEnabled = false;
        beginQuads();
        vertex(x0 + nx, y0 + ny);
        vertex(x0 - nx, y0 - ny);
        vertex(x1 - nx, y1 - ny);
        vertex(x1 + nx, y1 + ny);
        endQuads();
        texEnabled = prev;
    }

    public static void gradientRect(int x0, int y0, int x1, int y1, int argb1, int argb2) {
        boolean prev = texEnabled;
        texEnabled = false;
        beginQuads();
        vertex(x1, y0, 0, 0, argb1);
        vertex(x0, y0, 0, 0, argb1);
        vertex(x0, y1, 0, 0, argb2);
        vertex(x1, y1, 0, 0, argb2);
        endQuads();
        texEnabled = prev;
    }

    // ---- Text

    public static Font font() {
        return Minecraft.getInstance().font;
    }

    /**
     * Draws text whose height is approximately {@code size} pixels.
     */
    public static void text(String str, double x, double y, double size, int argb, boolean shadow) {
        if (str == null || str.isEmpty()) return;
        float s = (float) (size / font().lineHeight);
        pushMatrix();
        translate(x, y);
        scale(s, s);
        graphics().text(font(), str, 0, 0, argb, shadow);
        popMatrix();
    }

    public static double textWidth(String str, double size) {
        return font().width(str) * (size / font().lineHeight);
    }

    public static double charWidth(int chr, double size) {
        return font().width(new String(Character.toChars(chr))) * (size / font().lineHeight);
    }

    // ---- Item

    public static void item(ItemStack stack, double x, double y) {
        pushMatrix();
        translate(x, y);
        graphics().item(stack, 0, 0);
        popMatrix();
    }

    public static void itemWithDecorations(ItemStack stack, double x, double y) {
        pushMatrix();
        translate(x, y);
        graphics().item(stack, 0, 0);
        graphics().itemDecorations(font(), stack, 0, 0);
        popMatrix();
    }

    /**
     * Draws 3D content (through the legacy GL emulation) inside a screen rect. The rect is given in absolute GUI
     *  coordinates (the current pose isn't applied).
     * @param scale GUI pixels per unit
     */
    public static void pip(int x0, int y0, int x1, int y1, float scale, Runnable draw) {
        if (x1 <= x0 || y1 <= y0) return;
        ((GuiGraphicsExtractorAccessor) graphics()).lambdalib2$getGuiRenderState().addPicturesInPictureState(
            new LegacyPipRenderState(x0, y0, x1, y1, scale, draw, graphics().scissorStack.peek()));
    }

}
