package cn.lambdalib2.render.legacy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * A temporal wrapper that imitates the legacy tessellator, on top of the {@link LegacyGL} emulation.
 */
@Environment(EnvType.CLIENT)
public class Tessellator {

    public static final Tessellator instance = new Tessellator();

    private double _dx, _dy, _dz;

    public static Tessellator getInstance() {
        return instance;
    }

    public void startDrawing(int mode) {
        LegacyGL.begin(mode);
    }

    public void startDrawingQuads() {
        LegacyGL.begin(GL11.GL_QUADS);
    }

    public void draw() {
        LegacyGL.end();
        setTranslation(0, 0, 0);
    }

    public void setTranslation(double dx, double dy, double dz) {
        _dx = dx;
        _dy = dy;
        _dz = dz;
    }

    public void addVertex(double x, double y, double z) {
        LegacyGL.vertex(x + _dx, y + _dy, z + _dz);
    }

    public void addVertexWithUV(double x, double y, double z, double u, double v) {
        LegacyGL.texCoord(u, v);
        LegacyGL.vertex(x + _dx, y + _dy, z + _dz);
    }

    public void setColorOpaque_F(float r, float g, float b) {
        LegacyGL.color(r, g, b, 1);
    }

    public void setColorRGBA_F(float r, float g, float b, float a) {
        LegacyGL.color(r, g, b, a);
    }

    public void setNormal(double x, double y, double z) {
    }

    public void setBrightness(int brightness) {
    }

    private Tessellator() {}

}
