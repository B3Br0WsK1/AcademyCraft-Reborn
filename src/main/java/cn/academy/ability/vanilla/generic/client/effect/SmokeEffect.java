package cn.academy.ability.vanilla.generic.client.effect;

import cn.academy.Resources;
import cn.academy.entity.LocalEntity;
import cn.lambdalib2.render.legacy.Tessellator;
import cn.lambdalib2.util.EntityLook;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.RenderUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import static cn.lambdalib2.render.legacy.GL11.*;

/**
 * Ported from Scala.
 * @author WeAthFolD
 */
@Environment(EnvType.CLIENT)
public class SmokeEffect extends LocalEntity {

    private static Identifier texture;

    private final double initTime = time();

    public float rotation = 0.0f;
    public float size = 1.0f;

    private final float lifeModifier = 0.5f + rand.nextFloat() * 0.2f;

    private final float rotSpeed = 0.3f * (rand.nextFloat() + 3);

    final int frame = rand.nextInt(4);

    public SmokeEffect(Level world) {
        super(world);
    }

    @Override
    public void onUpdate() {
        rotation += rotSpeed;

        Vec3 m = getDeltaMovement();
        setPos(getX() + m.x, getY() + m.y, getZ() + m.z);

        if (deltaTime() >= 4f) {
            setDead();
        }
    }

    float alpha() {
        float dt = (float) (deltaTime() / lifeModifier);
        if (dt <= 0.3f) return dt / 0.3f;
        if (dt <= 1.5f) return 1.0f;
        if (dt <= 2) return 1 - (dt - 1.5f) / 0.5f;
        return 0.0f;
    }

    private double deltaTime() {
        return time() - initTime;
    }

    private static double time() {
        return GameTimer.getTime();
    }

    @Override
    public void renderEffect(double x, double y, double z, float pt) {
        if (texture == null) texture = Resources.preloadTexture("effects/smokes");
        // (x, y, z) is relative to the camera
        EntityLook look = new EntityLook(new Vec3(x, y, z));

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_ALPHA_TEST);
        glDisable(GL_CULL_FACE);
        glPushMatrix();
        glTranslated(x, y, z);

        glRotated(-look.yaw + 180, 0, 1, 0);
        glRotated(-look.pitch, 1, 0, 0);
        glScaled(size, size, 1);

        double u = (frame % 2) / 2.0, v = (frame / 2) / 2.0;

        Tessellator t = Tessellator.instance;
        glColor4f(1, 1, 1, alpha());
        RenderUtils.loadTexture(texture);

        t.startDrawingQuads();
        t.addVertexWithUV(-1, -1, 0, u, v);
        t.addVertexWithUV(-1, 1, 0, u, v + 0.5);
        t.addVertexWithUV(1, 1, 0, u + 0.5, v + 0.5);
        t.addVertexWithUV(1, -1, 0, u + 0.5, v);
        t.draw();

        glPopMatrix();
        glEnable(GL_CULL_FACE);
        glEnable(GL_ALPHA_TEST);
    }

}
