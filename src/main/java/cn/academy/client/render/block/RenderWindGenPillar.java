package cn.academy.client.render.block;
import cn.lambdalib2.render.legacy.GL11;
import cn.academy.Resources;
import cn.academy.block.tileentity.TileWindGenPillar;
import cn.lambdalib2.multiblock.RenderBlockMulti;
import cn.lambdalib2.render.obj.ObjLegacyRender;
import cn.lambdalib2.util.RenderUtils;
import cn.lambdalib2.render.legacy.TileEntitySpecialRenderer;
import net.minecraft.resources.Identifier;


public class RenderWindGenPillar extends TileEntitySpecialRenderer<TileWindGenPillar> {
    public static final RenderWindGenPillar instance = new RenderWindGenPillar();

    private ObjLegacyRender mdl = Resources.getModel("windgen_pillar");

    private Identifier texture = Resources.getTexture("models/windgen_pillar");

    @Override
    public void render(TileWindGenPillar te, double x, double y, double z, float partialTicks,
                       int destroyStage, float alpha) {
        RenderUtils.loadTexture(texture);
        GL11.glPushMatrix();
        GL11.glTranslated(x + .5, y, z + .5);
        mdl.renderAll();
        GL11.glPopMatrix();
    }

}