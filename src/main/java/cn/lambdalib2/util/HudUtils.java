/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of LambdaLib modding library.
* https://github.com/LambdaInnovation/LambdaLib
* Licensed under MIT, see project root for more information.
*/
package cn.lambdalib2.util;

import cn.lambdalib2.render.gui.GuiDraw;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * 2D drawing helpers. All methods draw into the current {@link GuiDraw} context with its current color/texture state.
 * @author WeAthFolD
 */
@Environment(EnvType.CLIENT)
public class HudUtils {

    public static double zLevel = 0;

    public static void pushZLevel() {
    }

    public static void popZLevel() {
    }

    public static void loadTexture(Identifier res) {
        GuiDraw.bindTexture(res);
    }

    public static void rect(double width, double height) {
        rect(0, 0, width, height);
    }

    public static void rect(double x, double y, double width, double height) {
        if (legacyWorld()) {
            worldQuad(x, y, 0, 0, width, height, 1, 1);
            return;
        }
        GuiDraw.rect(x, y, width, height);
    }

    /**
     * When drawing inside a legacy GL world context (e.g. a picture-in-picture element), rects are drawn in 3D.
     */
    private static boolean legacyWorld() {
        return cn.lambdalib2.render.legacy.LegacyGL.isWorldActive();
    }

    private static void worldQuad(double x, double y, double u, double v, double w, double h, double tw, double th) {
        cn.lambdalib2.render.legacy.LegacyGL.begin(cn.lambdalib2.render.legacy.GL11.GL_QUADS);
        cn.lambdalib2.render.legacy.LegacyGL.texCoord(u, v + th);
        cn.lambdalib2.render.legacy.LegacyGL.vertex(x, y + h, 0);
        cn.lambdalib2.render.legacy.LegacyGL.texCoord(u + tw, v + th);
        cn.lambdalib2.render.legacy.LegacyGL.vertex(x + w, y + h, 0);
        cn.lambdalib2.render.legacy.LegacyGL.texCoord(u + tw, v);
        cn.lambdalib2.render.legacy.LegacyGL.vertex(x + w, y, 0);
        cn.lambdalib2.render.legacy.LegacyGL.texCoord(u, v);
        cn.lambdalib2.render.legacy.LegacyGL.vertex(x, y, 0);
        cn.lambdalib2.render.legacy.LegacyGL.end();
    }

    public static void rect(double x, double y, double u, double v, double width, double height) {
        rect(x, y, u, v, width, height, width, height);
    }

    public static void rect(double x, double y, double u, double v, double width, double height, double texWidth, double texHeight) {
        if (legacyWorld()) {
            net.minecraft.resources.Identifier t = cn.lambdalib2.render.legacy.LegacyGL.getTexture();
            double tw = cn.lambdalib2.render.legacy.LegacyTextures.getSize(t, false), th = cn.lambdalib2.render.legacy.LegacyTextures.getSize(t, true);
            worldQuad(x, y, u / tw, v / th, width, height, texWidth / tw, texHeight / th);
            return;
        }
        GuiDraw.rect(x, y, u, v, width, height, texWidth, texHeight);
    }

    public static void rawRect(double x, double y, double u, double v, double width, double height, double texWidth, double texHeight) {
        if (legacyWorld()) {
            worldQuad(x, y, u, v, width, height, texWidth, texHeight);
            return;
        }
        GuiDraw.rawRect(x, y, u, v, width, height, texWidth, texHeight);
    }

    public static void colorRect(double x, double y, double width, double height) {
        if (legacyWorld()) {
            boolean tex = cn.lambdalib2.render.legacy.LegacyGL.isEnabled(cn.lambdalib2.render.legacy.GL11.GL_TEXTURE_2D);
            cn.lambdalib2.render.legacy.LegacyGL.disable(cn.lambdalib2.render.legacy.GL11.GL_TEXTURE_2D);
            worldQuad(x, y, 0, 0, width, height, 1, 1);
            if (tex) cn.lambdalib2.render.legacy.LegacyGL.enable(cn.lambdalib2.render.legacy.GL11.GL_TEXTURE_2D);
            return;
        }
        GuiDraw.colorRect(x, y, width, height);
    }

    public static void drawRectOutline(double x, double y, double w, double h, float lineWidth) {
        GuiDraw.drawRectOutline(x, y, w, h, lineWidth);
    }

    public static void drawHoveringText(List<String> lines, int x, int y, Font font, int width, int height) {
        if (lines.isEmpty()) return;
        int k = 0;
        for (String s : lines) {
            k = Math.max(k, font.width(s));
        }

        int i1 = x + 12;
        int j1 = y - 12;
        int k1 = 8;

        if (lines.size() > 1) {
            k1 += 2 + (lines.size() - 1) * 10;
        }

        if (i1 + k > width) {
            i1 -= 28 + k;
        }

        if (j1 + k1 + 6 > height) {
            j1 = height - k1 - 6;
        }

        int l1 = -267386864;
        drawGradientRect(i1 - 3, j1 - 4, i1 + k + 3, j1 - 3, l1, l1);
        drawGradientRect(i1 - 3, j1 + k1 + 3, i1 + k + 3, j1 + k1 + 4, l1, l1);
        drawGradientRect(i1 - 3, j1 - 3, i1 + k + 3, j1 + k1 + 3, l1, l1);
        drawGradientRect(i1 - 4, j1 - 3, i1 - 3, j1 + k1 + 3, l1, l1);
        drawGradientRect(i1 + k + 3, j1 - 3, i1 + k + 4, j1 + k1 + 3, l1, l1);
        int i2 = 1347420415;
        int j2 = (i2 & 16711422) >> 1 | i2 & -16777216;
        drawGradientRect(i1 - 3, j1 - 3 + 1, i1 - 3 + 1, j1 + k1 + 3 - 1, i2, j2);
        drawGradientRect(i1 + k + 2, j1 - 3 + 1, i1 + k + 3, j1 + k1 + 3 - 1, i2, j2);
        drawGradientRect(i1 - 3, j1 - 3, i1 + k + 3, j1 - 3 + 1, i2, i2);
        drawGradientRect(i1 - 3, j1 + k1 + 2, i1 + k + 3, j1 + k1 + 3, j2, j2);

        for (int k2 = 0; k2 < lines.size(); ++k2) {
            GuiDraw.graphics().text(font, lines.get(k2), i1, j1, -1, true);
            if (k2 == 0) {
                j1 += 2;
            }
            j1 += 10;
        }
    }

    /**
     * Draws a vertical gradient. Colors are ARGB.
     */
    public static void drawGradientRect(int x0, int y0, int x1, int y1, int color1, int color2) {
        GuiDraw.gradientRect(x0, y0, x1, y1, color1, color2);
    }

}
