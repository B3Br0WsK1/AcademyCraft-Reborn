package cn.academy.client.render.entity;
import cn.lambdalib2.render.legacy.GL11;
import cn.lambdalib2.render.legacy.GL20;
import cn.academy.client.render.util.SimpleModelBiped;
import cn.academy.Resources;
import cn.academy.entity.EntityTPMarking;
import cn.lambdalib2.render.legacy.ShaderSimple;
import cn.lambdalib2.util.RenderUtils;
import cn.lambdalib2.render.legacy.LegacyRender;
import cn.lambdalib2.render.legacy.RenderManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.Identifier;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import net.fabricmc.api.EnvType;



@Environment(EnvType.CLIENT)
public class MarkRender extends LegacyRender<EntityTPMarking> {

    {
        shadowOpaque = 0;
    }

    protected Identifier[] tex = Resources.getEffectSeq("tp_mark", 7);
    protected SimpleModelBiped model = new SimpleModelBiped();

    public MarkRender(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    public void doRender(EntityTPMarking mark, double x, double y, double z, float var8, float var9) {
        if (RenderUtils.isInShadowPass())
            return;
        if (!mark.firstUpdated())
            return;

        int texID = (int) ((mark.tickCount / 2.5) % tex.length);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        GL11.glPushMatrix();
        {
            GL11.glTranslated(x, y, z);

            GL11.glRotated(-mark.getYRot(), 0, 1, 0);
            GL11.glScaled(-1, -1, 1);
            ShaderSimple.instance().useProgram();
            RenderUtils.loadTexture(tex[texID]);

            if (!mark.available) {
                GL11.glColor4d(1, 0.2, 0.2, 1);
            } else {
                GL11.glColor4d(1, 1, 1, 1);
            }

            model.draw();
            GL20.glUseProgram(0);
        }
        GL11.glPopMatrix();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_CULL_FACE);
    }

    @Nullable
    @Override
    protected Identifier getEntityTexture(EntityTPMarking entity) {
        return null;
    }

}