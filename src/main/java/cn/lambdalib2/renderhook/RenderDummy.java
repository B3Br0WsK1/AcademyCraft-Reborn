package cn.lambdalib2.renderhook;

import cn.lambdalib2.render.legacy.LegacyEntityRenderer;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.ViewOptimize;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import static cn.lambdalib2.render.legacy.GL11.*;

/**
 * @author WeAthFolD
 */
@Environment(EnvType.CLIENT)
public class RenderDummy extends LegacyEntityRenderer<EntityDummy> {

    public RenderDummy(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void doRender(EntityDummy entity, double x, double y, double z, float b) {
        if (entity.player == null || !(entity.data instanceof DummyRenderData data)) return;
        glPushMatrix();
        glTranslated(x, y, z);

        boolean fp = ViewOptimize.isFirstPerson(entity);

        float yy, ly;
        if(fp) {
            yy = entity.rotationYawHead;
            ly = entity.lastRotationYawHead;
        } else {
            yy = entity.getYRot();
            ly = entity.lastRotationYaw;
        }

        float yaw = MathUtils.lerpf(ly, yy, b);
        glRotated(180 - yaw, 0, 1, 0);

        // Render hand

        if(fp) {
            glTranslated(0, entity.player.getEyeHeight(), 0);
            glRotated(-entity.getXRot(), 1, 0, 0);
        } else {
            ViewOptimize.fixThirdPerson();
            glTranslated(0, 1.6, 0);
        }

        for(PlayerRenderHook hook : data.getRenderers()) {
            hook.renderHand(fp);
        }
        glPopMatrix();
    }

}
