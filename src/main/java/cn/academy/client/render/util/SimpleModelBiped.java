package cn.academy.client.render.util;

import cn.lambdalib2.render.legacy.GL11;
import cn.lambdalib2.render.legacy.LegacyGL;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * A static humanoid model (the 1.12 ModelBiped in its default pose), drawn through the legacy GL emulation with the
 *  currently bound texture (64x32 skin layout).
 * @author WeathFolD
 *
 */
@Environment(EnvType.CLIENT)
public class SimpleModelBiped {

    private static final float TEX_W = 64, TEX_H = 32;

    public SimpleModelBiped() {}

    public void draw() {
        float s = 0.0625f;
        box(0, 0, 0, -4, -8, -4, 8, 8, 8, 0, 0, s, false);        // head
        box(0, 0, 0, -4, 0, -2, 8, 12, 4, 16, 16, s, false);      // body
        box(-5, 2, 0, -3, -2, -2, 4, 12, 4, 40, 16, s, false);    // right arm
        box(5, 2, 0, -1, -2, -2, 4, 12, 4, 40, 16, s, true);      // left arm
        box(-1.9f, 12, 0, -2, 0, -2, 4, 12, 4, 0, 16, s, false);  // right leg
        box(1.9f, 12, 0, -2, 0, -2, 4, 12, 4, 0, 16, s, true);    // left leg
        // headwear (slightly inflated)
        box(0, 0, 0, -4.5f, -8.5f, -4.5f, 9, 9, 9, 32, 0, s, false, 8, 8, 8);
    }

    private static void box(float px, float py, float pz, float x0, float y0, float z0, float w, float h, float d,
                            int u, int v, float scale, boolean mirror) {
        box(px, py, pz, x0, y0, z0, w, h, d, u, v, scale, mirror, w, h, d);
    }

    /**
     * Draws a box. (tw, th, td) are the texture dimensions of the box (may differ from geometry when inflated).
     */
    private static void box(float px, float py, float pz, float x0, float y0, float z0, float w, float h, float d,
                            int u, int v, float scale, boolean mirror, float tw, float th, float td) {
        float x1 = x0 + w, y1 = y0 + h, z1 = z0 + d;
        x0 += px; x1 += px; y0 += py; y1 += py; z0 += pz; z1 += pz;
        if (mirror) {
            float t = x0; x0 = x1; x1 = t;
        }
        GL11.glPushMatrix();
        GL11.glScaled(scale, scale, scale);

        // +x face
        face(x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, u + td + tw, v + td, td, th);
        // -x face
        face(x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1, u, v + td, td, th);
        // -y face (top)
        face(x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, u + td, v, tw, td);
        // +y face (bottom)
        face(x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, u + td + tw, v, tw, td);
        // -z face (front)
        face(x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, u + td, v + td, tw, th);
        // +z face (back)
        face(x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1, u + td + tw + td, v + td, tw, th);

        GL11.glPopMatrix();
    }

    private static void face(float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float u, float v, float uw, float vh) {
        float u0 = u / TEX_W, u1 = (u + uw) / TEX_W, v0 = v / TEX_H, v1 = (v + vh) / TEX_H;
        LegacyGL.begin(GL11.GL_QUADS);
        LegacyGL.texCoord(u0, v0); LegacyGL.vertex(ax, ay, az);
        LegacyGL.texCoord(u1, v0); LegacyGL.vertex(bx, by, bz);
        LegacyGL.texCoord(u1, v1); LegacyGL.vertex(cx, cy, cz);
        LegacyGL.texCoord(u0, v1); LegacyGL.vertex(dx, dy, dz);
        LegacyGL.end();
    }

}
