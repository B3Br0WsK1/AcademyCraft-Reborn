package cn.lambdalib2.template.client.render;

import cn.lambdalib2.render.legacy.GL11;
import cn.lambdalib2.render.legacy.LegacyGL;
import cn.lambdalib2.render.legacy.LegacyRender;
import cn.lambdalib2.render.legacy.RenderManager;
import cn.lambdalib2.render.legacy.Tessellator;
import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.RenderUtils;
import cn.lambdalib2.util.ViewOptimize;
import cn.lambdalib2.util.ViewOptimize.IAssociatePlayer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

/**
 * A EntityRender that renders an entity as a single icon (billboard).
 */
@Environment(EnvType.CLIENT)
public class RenderIcon<T extends Entity> extends LegacyRender<T> {

    protected Identifier icon;
    protected float size = 0.5F;
    protected boolean hasLight = false;
    public final Color color = Colors.white();

    protected float minTolerateAlpha = 0.0F;

    public RenderIcon(RenderManager manager, Identifier ic) {
        super(manager);
        icon = ic;
    }

    public RenderIcon<T> setSize(float s) {
        size = s;
        return this;
    }

    public RenderIcon<T> setHasLight(boolean b) {
        hasLight = b;
        return this;
    }

    @Override
    public void doRender(T par1Entity, double par2, double par4, double par6, float par8, float partialTicks) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glPushMatrix(); {
            GL11.glTranslatef((float) par2, (float) par4, (float) par6);
            GL11.glScalef(size, size, size);
            postTranslate(par1Entity);
            if(icon != null) {
                RenderUtils.loadTexture(icon);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
            } else {
                GL11.glDisable(GL11.GL_TEXTURE_2D);
            }

            drawIcon(par1Entity, Tessellator.instance);

            GL11.glEnable(GL11.GL_TEXTURE_2D);
        } GL11.glPopMatrix();
    }

    protected void postTranslate(Entity ent) {}

    protected void firstTranslate(Entity ent) {}

    private void drawIcon(Entity e, Tessellator tessllator) {
        float f4 = 1.0F;
        float f5 = 0.5F;
        float f6 = 0.25F;
        GL11.glRotatef(180F - renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        firstTranslate(e);
        LegacyGL.color(color.getRedF(), color.getGreenF(), color.getBlueF(), color.getAlphaF());
        tessllator.startDrawingQuads();

        if(e instanceof IAssociatePlayer) {
            ViewOptimize.fix((IAssociatePlayer) e);
        }
        tessllator.addVertexWithUV(0.0F - f5, 0.0F - f6, 0.0D, 0, 1);
        tessllator.addVertexWithUV(f4 - f5, 0.0F - f6, 0.0D, 1, 1);
        tessllator.addVertexWithUV(f4 - f5, f4 - f6, 0.0D, 1, 0);
        tessllator.addVertexWithUV(0.0F - f5, f4 - f6, 0.0D, 0, 0);

        tessllator.draw();
    }

}
