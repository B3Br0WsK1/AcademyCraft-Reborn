package cn.academy.ability.vanilla.vecmanip.client.effect;

import cn.academy.Resources;
import cn.lambdalib2.util.RenderUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;

import static cn.academy.util.ImprovedNoise.noise;
import static cn.lambdalib2.render.legacy.GL11.*;

/**
 * Ported from Scala.
 */
@Environment(EnvType.CLIENT)
public final class TornadoRenderer {

    private TornadoRenderer() {}

    public static final Identifier texture = Resources.getTexture("effects/tornado_ring");

    static final int div = 20;
    static final double uStep = 1.0 / div;
    static final double pi2 = Math.PI * 2;
    static final double[][] circleData = new double[div][];

    static {
        for (int x = 0; x < div; ++x) {
            double rad = (double) x / div * pi2;
            circleData[x] = new double[] { Math.sin(rad), Math.cos(rad) };
        }
    }

    private static void calcdx(double ny, double tinput, double[] target) {
        double t = tinput * 0.1;
        target[0] = noise(ny, t) * (0.3 + Math.pow(ny * 2, 1.4));
        target[1] = noise(ny, t, 1) * (0.3 + Math.pow(ny * 2, 1.4));
    }

    private static double r(double ny, double t) {
        return (0.5 + 0.3 * noise(ny, 0.2 * t)) + 0.5 * Math.pow(1.5 * ny, 2) + noise(ny);
    }

    private static double rot(double ny, double t) {
        return 0.1 * (1 + 0.5 * ny) * t;
    }

    private static void drawRing(double y, double w, double[] vdx, double r, double rot) {
        double dx = vdx[0];
        double dz = vdx[1];

        for (int idx = 0; idx < div; ++idx) {
            double[] v0 = circleData[idx];
            double[] v1 = circleData[(idx + 1) % div];

            double x0 = v0[0] * r;
            double z0 = v0[1] * r;
            double x1 = v1[0] * r;
            double z1 = v1[1] * r;

            double y0 = y + w / 2;
            double y1 = y - w / 2;
            double u0 = uStep * idx - rot;
            double u1 = u0 + uStep;
            glTexCoord2d(u0, 0);
            glVertex3d(x0 + dx, y0, z0 + dz);

            glTexCoord2d(u0, 1);
            glVertex3d(x0 + dx, y1, z0 + dz);

            glTexCoord2d(u1, 1);
            glVertex3d(x1 + dx, y1, z1 + dz);

            glTexCoord2d(u1, 0);
            glVertex3d(x1 + dx, y0, z1 + dz);
        }
    }

    public static void doRender(TornadoEffect eff) {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        RenderUtils.loadTexture(texture);
        glPushMatrix();

        glDepthMask(false);
        glDisable(GL_CULL_FACE);
        glColor4d(1, 1, 1, eff.alpha * 0.7);
        glBegin(GL_QUADS);

        double[] vdx = { 0.0, 0.0 };

        double time = eff.time();
        for (TornadoEffect.Ring ring : eff.getRings()) {
            double ny = ring.y() / eff.ht;
            calcdx(ny, time, vdx);
            vdx[0] = vdx[0] * eff.sz * eff.dscale;
            vdx[1] = vdx[1] * eff.sz * eff.dscale;
            double vr = r(ny, time) * eff.sz * ring.sizeScale();
            drawRing(ring.y(), ring.width(), vdx, vr, rot(ny, time) + ring.phase());
        }

        glEnd();
        glColor4f(1, 1, 1, 1);
        glEnable(GL_CULL_FACE);
        glDepthMask(true);
        glPopMatrix();
    }

}
