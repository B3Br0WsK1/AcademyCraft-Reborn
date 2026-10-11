package cn.lambdalib2.particle;

import cn.lambdalib2.render.legacy.LegacyEntityRenderer;
import cn.lambdalib2.render.legacy.LegacyGL;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import static cn.lambdalib2.render.legacy.GL11.*;

/**
 * A delegator renderer for Sprite and Entities that implements ISpriteEntity.
 *
 * @author WeAthFolD
 */
@Environment(EnvType.CLIENT)
public class RenderParticle extends LegacyEntityRenderer<Particle> {

    static Sprite sprite = new Sprite();

    public RenderParticle(EntityRendererProvider.Context m) {
        super(m);
    }

    @Override
    public void doRender(Particle ish, double x, double y, double z, float partialTicks) {
        if (!ish.isUpdated())
            return;

        ish.updateSprite(sprite);

        glPushMatrix();

        if (ish.needViewOptimize()) {
            glTranslated(0, -0.2, 0);
        }

        glTranslated(x, y, z);
        if (ish.customRotation) {
            glRotatef(ish.getYRot(), 0, 1, 0);
            glRotatef(ish.getXRot(), 0, 0, 1);
        } else {
            glRotatef(180F - LegacyGL.cameraYaw(), 0.0F, 1.0F, 0.0F);
            glRotatef(-LegacyGL.cameraPitch(), 1.0F, 0.0F, 0.0F);
        }

        sprite.draw();

        glPopMatrix();
    }

}
