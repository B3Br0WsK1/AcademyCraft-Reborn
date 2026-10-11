package cn.academy.ability.vanilla.vecmanip.client.effect;

import cn.academy.Resources;
import cn.academy.ability.context.Context.Status;
import cn.academy.ability.vanilla.vecmanip.skill.VecAccel.VecAccelContext;
import cn.academy.entity.LocalEntity;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.RenderUtils;
import cn.lambdalib2.util.VecUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static cn.lambdalib2.render.legacy.GL11.*;

/**
 * Parabola preview of VecAccel. Ported from Scala.
 */
@Environment(EnvType.CLIENT)
public class ParabolaEffect extends LocalEntity {

    private static final Identifier texture = Resources.getTexture("effects/glow_line");

    public final VecAccelContext ctx;

    public boolean canPerform = false;

    private final List<Vec3> vertices = new ArrayList<>();

    public ParabolaEffect(VecAccelContext ctx) {
        super(ctx.player.level());
        this.ctx = ctx;
        setPos(ctx.player.getX(), ctx.player.getY(), ctx.player.getZ());
    }

    @Override
    public void onUpdate() {
        setPos(ctx.player.getX(), ctx.player.getY(), ctx.player.getZ());
        canPerform = ctx.canPerform;
        if (ctx.getStatus() == Status.TERMINATED) {
            setDead();
        }
    }

    @Override
    public void renderEffect(double x, double y, double z, float partialTicks) {
        if (!Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;

        Vec3 speed = ctx.initSpeed(partialTicks);
        Player player = ctx.player;

        float yawLerp = MathUtils.lerpf(player.yRotO, player.getYRot(), partialTicks);
        float pitchLerp = MathUtils.lerpf(player.xRotO, player.getXRot(), partialTicks);

        Vec3 lookFix = VecUtils.toDirVector(yawLerp, pitchLerp);
        Vec3 lookRot = new Vec3(lookFix.x, 0, lookFix.z);
        lookRot = lookRot.yRot(90);
        lookRot = VecUtils.multiply(lookRot.normalize(), -0.08);
        lookRot = new Vec3(lookRot.x, 1.56, lookRot.z);

        Vec3 pos = VecUtils.subtract(lookRot, VecUtils.multiply(lookFix, 0.12));

        vertices.clear();

        double dt = 0.02;

        for (int i = 0; i < 100; ++i) {
            vertices.add(pos);
            speed = VecUtils.multiply(speed, 0.98);
            pos = pos.add(VecUtils.multiply(speed, dt));
            speed = new Vec3(speed.x, speed.y - dt * 1.9, speed.z);
        }

        glPushMatrix();
        glTranslated(x, y, z);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        RenderUtils.loadTexture(texture);
        glDisable(GL_CULL_FACE);
        glDisable(GL_ALPHA_TEST);
        glColor4f(1, 1, 1, 0.6f);
        for (int idx = 1; idx < vertices.size(); ++idx) {
            double h = .02;

            Vec3 prev = vertices.get(idx - 1), cur = vertices.get(idx);

            float alpha = 0.7f * (1 - idx * 0.03f);
            if (canPerform) {
                glColor4f(1, 1, 1, alpha);
            } else {
                glColor4f(1, 0.2f, 0.2f, alpha);
            }

            glBegin(GL_QUADS);
            glTexCoord2d(0, 0);
            glVertex3d(prev.x, prev.y + h, prev.z);

            glTexCoord2d(0, 1);
            glVertex3d(prev.x, prev.y - h, prev.z);

            glTexCoord2d(1, 1);
            glVertex3d(cur.x, cur.y - h, cur.z);

            glTexCoord2d(1, 0);
            glVertex3d(cur.x, cur.y + h, cur.z);
            glEnd();
        }
        glEnable(GL_ALPHA_TEST);
        glEnable(GL_CULL_FACE);
        glPopMatrix();
    }

}
