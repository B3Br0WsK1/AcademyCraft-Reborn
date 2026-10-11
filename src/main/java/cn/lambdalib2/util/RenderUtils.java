package cn.lambdalib2.util;

import cn.lambdalib2.render.gui.GuiDraw;
import cn.lambdalib2.render.legacy.GL11;
import cn.lambdalib2.render.legacy.LegacyGL;
import cn.lambdalib2.render.legacy.Tessellator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Utils for legacy GL rendering, working on top of the {@link LegacyGL} emulation.
 * @author WeAthFolD
 */
@Environment(EnvType.CLIENT)
public class RenderUtils {

    private static Identifier textureState;

    /**
     * Stores the current texture state. stack depth: 1
     */
    public static void pushTextureState() {
        textureState = LegacyGL.getTexture();
    }

    /**
     * Restores the stored texture state. stack depth: 1
     */
    public static void popTextureState() {
        LegacyGL.bindTexture(textureState);
        textureState = null;
    }

    public static void glVertexAndUV(Vec3 vertex, double u, double v) {
        LegacyGL.texCoord(u, v);
        LegacyGL.vertex(vertex.x, vertex.y, vertex.z);
    }

    public static void glVertexAndUV(double x, double y, double z, double u, double v) {
        LegacyGL.texCoord(u, v);
        LegacyGL.vertex(x, y, z);
    }

    public static void glVertex(Vec3 vertex) {
        LegacyGL.vertex(vertex.x, vertex.y, vertex.z);
    }

    public static void glTranslate(Vec3 v) {
        LegacyGL.translate(v.x, v.y, v.z);
    }

    public static void glScale(Vec3 v) {
        LegacyGL.scale(v.x, v.y, v.z);
    }

    public static void glRotate(double angle, Vec3 axis) {
        LegacyGL.rotate(angle, axis.x, axis.y, axis.z);
    }

    /**
     * Binds the texture for both the legacy GL emulation and the GUI drawing.
     */
    public static void loadTexture(Identifier src) {
        LegacyGL.bindTexture(src);
        if (GuiDraw.isActive() && !LegacyGL.isWorldActive()) {
            GuiDraw.bindTexture(src);
        }
    }

    public static void drawEquippedItem(ItemStack stackToRender, double width) {
        // Not supported in 26.x; items are rendered through item models.
    }

    public static void drawEquippedItem(double width, Identifier texture) {
        drawEquippedItem(width, texture, texture);
    }

    public static void drawEquippedItemOverlay(double width, Identifier texture) {
        drawEquippedItem(width, texture, texture, 0, 0, 1, 1, true);
    }

    public static void drawEquippedItem(double width, Identifier front, Identifier back) {
        drawEquippedItem(width, front, back, 0, 0, 1, 1, false);
    }

    private static void drawEquippedItem(double w, Identifier front, Identifier back,
            double u1, double v1, double u2, double v2, boolean faceOnly) {
        Tessellator t = Tessellator.instance;
        Vec3 a1 = new Vec3(0, 0, w),
            a2 = new Vec3(1, 0, w),
            a3 = new Vec3(1, 1, w),
            a4 = new Vec3(0, 1, w),
            a5 = new Vec3(0, 0, -w),
            a6 = new Vec3(1, 0, -w),
            a7 = new Vec3(1, 1, -w),
            a8 = new Vec3(0, 1, -w);

        GL11.glPushMatrix();

        loadTexture(back);
        t.startDrawingQuads();
        addVertex(a1, u2, v2);
        addVertex(a2, u1, v2);
        addVertex(a3, u1, v1);
        addVertex(a4, u2, v1);
        t.draw();

        loadTexture(front);
        t.startDrawingQuads();
        addVertex(a8, u2, v1);
        addVertex(a7, u1, v1);
        addVertex(a6, u1, v2);
        addVertex(a5, u2, v2);
        t.draw();

        int tileSize = 32;
        float tx = 1.0f / (32 * tileSize);

        if(!faceOnly) {
            t.startDrawingQuads();
            for (int i = 0; i < tileSize; ++i) {
                float f = (float) i / tileSize;
                double u = u2 - (u2 - u1) * f - tx;
                t.addVertexWithUV(f, 0.0D, -w, u, v2);
                t.addVertexWithUV(f, 0.0D, w, u, v2);
                t.addVertexWithUV(f, 1.0D, w, u, v1);
                t.addVertexWithUV(f, 1.0D, -w, u, v1);
            }
            t.draw();
        }

        GL11.glPopMatrix();
    }

    private static void addVertex(Vec3 vec, double u, double v) {
        Tessellator.instance.addVertexWithUV(vec.x, vec.y, vec.z, u, v);
    }

    public static void addVertexLegacy(Vec3 vertex, double u, double v) {
        LegacyGL.texCoord(u, v);
        LegacyGL.vertex(vertex.x, vertex.y, vertex.z);
    }

    public static void renderItemInventory(ItemStack item) {
        if (GuiDraw.isActive()) {
            GuiDraw.graphics().item(item, 0, 0);
        }
    }

    /**
     * Shadow passes of shader mods aren't supported.
     */
    public static boolean isInShadowPass() {
        return false;
    }

    /**
     * Draws a translucent black overlay on the whole screen (GUI context only).
     */
    public static void drawBlackout() {
        if (!GuiDraw.isActive()) return;
        Minecraft mc = Minecraft.getInstance();
        GuiDraw.pushMatrix();
        GuiDraw.color4d(0, 0, 0, 0.7);
        GuiDraw.colorRect(0, 0, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        GuiDraw.resetColor();
        GuiDraw.popMatrix();
    }

}
