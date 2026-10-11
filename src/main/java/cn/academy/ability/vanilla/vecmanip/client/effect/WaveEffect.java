package cn.academy.ability.vanilla.vecmanip.client.effect;

import cn.academy.Resources;
import cn.academy.entity.LocalEntity;
import cn.lambdalib2.render.legacy.LegacyMesh;
import cn.lambdalib2.render.legacy.LegacyMeshUtils;
import cn.lambdalib2.render.legacy.SimpleMaterial;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.vis.curve.CubicCurve;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

import static cn.lambdalib2.render.legacy.GL11.*;
import static cn.lambdalib2.util.MathUtils.clampd;

/**
 * Ported from Scala.
 */
@Environment(EnvType.CLIENT)
public class WaveEffect extends LocalEntity {

    public static class Ring {
        public final int life;
        public double offset;
        public final double size;
        public final int timeOffset;

        Ring(int life, double offset, double size, int timeOffset) {
            this.life = life;
            this.offset = offset;
            this.size = size;
            this.timeOffset = timeOffset;
        }
    }

    // ---- Render resources ----
    private static CubicCurve alphaCurve, sizeCurve;
    private static LegacyMesh mesh;
    private static SimpleMaterial material;

    private static void initRender() {
        if (mesh != null) return;
        alphaCurve = new CubicCurve();
        alphaCurve.addPoint(0, 0);
        alphaCurve.addPoint(0.2, 1);
        alphaCurve.addPoint(0.5, 1);
        alphaCurve.addPoint(0.8, 1);
        alphaCurve.addPoint(1, 0);

        mesh = new LegacyMesh();
        material = new SimpleMaterial(Resources.getTexture("effects/glow_circle")).setIgnoreLight();
        LegacyMeshUtils.createBillboard(mesh, -.5, -.5, 1, 1);

        sizeCurve = new CubicCurve();
        sizeCurve.addPoint(0, 0.4);
        sizeCurve.addPoint(0.2, 0.8);
        sizeCurve.addPoint(2.5, 1.5);
    }

    public final int rings;
    public final double size;
    public final List<Ring> ringList = new ArrayList<>();
    public final int life = 15;

    public WaveEffect(Level world, int rings, double size) {
        super(world);
        this.rings = rings;
        this.size = size;

        for (int idx = 0; idx < rings; ++idx) {
            ringList.add(new Ring(
                RandUtils.rangei(8, 12),
                idx * 1.5 + RandUtils.ranged(-.3, .3),
                size * RandUtils.ranged(0.8, 1.2),
                idx * 2 + RandUtils.rangei(-1, 1)));
        }
    }

    @Override
    public void onUpdate() {
        if (tickCount >= life) {
            setDead();
        }
    }

    @Override
    public void renderEffect(double x, double y, double z, float partialTicks) {
        initRender();
        double maxAlpha = clampd(0, 1, alphaCurve.valueAt((double) tickCount / life));

        glDisable(GL_CULL_FACE);
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glPushMatrix();
        glTranslated(x, y, z);
        glRotated(-getYRot(), 0, 1, 0);
        glRotated(getXRot(), 1, 0, 0);

        double zOffset = tickCount / 40.0;

        glTranslated(0, 0, zOffset);

        for (Ring ring : ringList) {
            float alpha = (float) clampd(0, 1, alphaCurve.valueAt((double) (tickCount - ring.timeOffset) / ring.life));
            float realAlpha = (float) Math.min(maxAlpha, alpha);

            if (realAlpha > 0) {
                glPushMatrix();
                glTranslated(0, 0, ring.offset);

                double sizeScale = sizeCurve.valueAt(MathUtils.clampd(0, 1.62, tickCount / 20.0));
                glScaled(ring.size * sizeScale, ring.size * sizeScale, 1);
                material.color.setAlpha(Colors.f2i(realAlpha * 0.7f));
                mesh.draw(material);
                glPopMatrix();
            }
        }

        glPopMatrix();
        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
    }

}
