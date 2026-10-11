package cn.academy.ability.vanilla.vecmanip.client.effect;

import cn.academy.Resources;
import cn.academy.ability.context.Context.Status;
import cn.academy.ability.vanilla.vecmanip.skill.PlasmaCannon.PlasmaCannonContext;
import cn.academy.entity.LocalEntity;
import cn.lambdalib2.render.legacy.LegacyGL;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.RenderUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static cn.lambdalib2.render.legacy.GL11.*;
import static cn.lambdalib2.util.RandUtils.rangef;
import static cn.lambdalib2.util.RandUtils.rangei;

/**
 * Plasma body of PlasmaCannon. Ported from Scala.
 * The original rendered the balls as metaballs with the plasma_body shader; this port draws each ball as a glowing
 *  camera-facing billboard instead.
 */
@Environment(EnvType.CLIENT)
public class PlasmaBodyEffect extends LocalEntity {

    private static final Identifier TEXTURE = Resources.getTexture("effects/glow_circle");

    public record TrigPar(float amp, float speed, float dphase) {
        public float phase(float time) {
            return speed * time - dphase;
        }
    }

    public record BallInst(float size, Vector3f center, TrigPar hmove, TrigPar vmove) {}

    public final PlasmaCannonContext ctx;
    public final List<BallInst> balls = new ArrayList<>();

    private double initTime = GameTimer.getTime();
    private double startTime = GameTimer.getTime();
    public float alpha = 0.0f;

    public PlasmaBodyEffect(Level world, PlasmaCannonContext ctx) {
        super(world);
        this.ctx = ctx;

        for (int i = 0; i < 4; ++i) {
            balls.add(new BallInst(rangef(1, 1.5f),
                new Vector3f(rangef(-1.5f, 1.5f), rangef(-1.5f, 1.5f), rangef(-1.5f, 1.5f)),
                nextTrigPar(1.0f),
                nextTrigPar(1.0f)));
        }
        int n = rangei(4, 6);
        for (int i = 0; i < n; ++i) {
            balls.add(new BallInst(rangef(0.1f, 0.3f),
                new Vector3f(rangef(-3f, 3f), rangef(-3f, 3f), rangef(-3f, 3f)),
                nextTrigPar(2.5f),
                nextTrigPar(2.5f)));
        }
    }

    private static TrigPar nextTrigPar(float size) {
        float amp = rangef(1.4f, 2f) * size;
        float speed = rangef(0.5f, 0.7f);
        float dphase = rangef(0, MathUtils.PI_F * 2);
        return new TrigPar(amp, speed, dphase);
    }

    public double deltaTime() {
        return GameTimer.getTime() - initTime;
    }

    @Override
    public void onUpdate() {
        boolean terminated = ctx.getStatus() == Status.TERMINATED;
        if (terminated && Math.abs(alpha) <= 1e-3f) {
            setDead();
        }
    }

    public void updateAlpha() {
        double dt = deltaTime();
        boolean terminated = ctx.getStatus() == Status.TERMINATED;
        float desiredAlpha = terminated ? 0 : 1;

        alpha = moveTowards(alpha, desiredAlpha, (float) dt * (terminated ? 1f : 0.3f));

        initTime = GameTimer.getTime();
    }

    private static float moveTowards(float from, float to, float max) {
        float delta = to - from;
        return from + Math.min(Math.abs(delta), max) * Math.signum(delta);
    }

    @Override
    public void renderEffect(double x, double y, double z, float partialTicks) {
        float time = (float) (GameTimer.getTime() - startTime);

        updateAlpha();
        float a = alpha * alpha;
        if (a <= 0) return;

        glDepthMask(false);
        glEnable(GL_BLEND);
        glDisable(GL_ALPHA_TEST);
        glDisable(GL_CULL_FACE);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);
        RenderUtils.loadTexture(TEXTURE);

        float yaw = LegacyGL.cameraYaw(), pitch = LegacyGL.cameraPitch();

        for (BallInst ball : balls) {
            float hrphase = ball.hmove().phase(time);
            float vtphase = ball.vmove().phase(time);

            float dx = ball.hmove().amp() * Mth.sin(hrphase);
            float dy = ball.vmove().amp() * Mth.sin(vtphase);
            float dz = ball.hmove().amp() * Mth.cos(hrphase);

            glPushMatrix();
            glTranslated(x + ball.center().x + dx, y + ball.center().y + dy, z + ball.center().z + dz);
            glRotated(-yaw, 0, 1, 0);
            glRotated(pitch, 1, 0, 0);
            double s = ball.size() * 3;
            glScaled(s, s, s);

            // Outer glow
            glColor4f(1.0f, 0.75f, 0.45f, 0.6f * a);
            quad(1.0);
            // Bright core
            glColor4f(1.0f, 1.0f, 0.95f, 0.8f * a);
            quad(0.45);

            glPopMatrix();
        }

        glColor4f(1, 1, 1, 1);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_CULL_FACE);
        glEnable(GL_ALPHA_TEST);
        glDepthMask(true);
    }

    private static void quad(double r) {
        glBegin(GL_QUADS);
        glTexCoord2d(0, 0);
        glVertex3d(-r, -r, 0);
        glTexCoord2d(0, 1);
        glVertex3d(-r, r, 0);
        glTexCoord2d(1, 1);
        glVertex3d(r, r, 0);
        glTexCoord2d(1, 0);
        glVertex3d(r, -r, 0);
        glEnd();
    }

}
