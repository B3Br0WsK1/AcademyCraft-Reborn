package cn.academy.client.render.entity;
import cn.lambdalib2.render.legacy.GL11;
import cn.academy.Resources;
import cn.academy.entity.EntityMagHook;
import cn.lambdalib2.render.obj.ObjLegacyRender;
import cn.lambdalib2.util.RenderUtils;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import cn.lambdalib2.render.legacy.LegacyRender;
import cn.lambdalib2.render.legacy.RenderManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.Identifier;
import net.fabricmc.api.EnvType;

//import net.minecraftforge.client.model.IModelCustom;

@Environment(EnvType.CLIENT)
public class RendererMagHook extends LegacyRender {

    final ObjLegacyRender model = Resources.getModel("maghook"),
            model_open = Resources.getModel("maghook_open");
    
    final Identifier texture = Resources.getTexture("models/maghook");

    public RendererMagHook(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    public void doRender(Entity ent, double x, double y, double z, float a,
            float b) {
        EntityMagHook hook = (EntityMagHook) ent;
        ObjLegacyRender realModel = model;
        if (hook.isHit) {
            realModel = model_open;
            hook.preRender();
            x = hook.getX() - renderManager.viewerPosX;
            y = hook.getY() - renderManager.viewerPosY;
            z = hook.getZ() - renderManager.viewerPosZ;
        }
        
        GL11.glPushMatrix();
        RenderUtils.loadTexture(texture);
        GL11.glTranslated(x, y, z);
        GL11.glRotated(-hook.getYRot() + 90, 0, 1, 0);
        GL11.glRotated(hook.getXRot() - 90, 0, 0, 1);
        double scale = 0.0054;
        GL11.glScaled(scale, scale, scale);
        realModel.renderAll();
        GL11.glPopMatrix();
    }

    @Override
    protected Identifier getEntityTexture(Entity var1) {
        return null;
    }

}