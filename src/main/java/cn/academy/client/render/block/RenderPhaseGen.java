package cn.academy.client.render.block;
import cn.lambdalib2.render.legacy.GL11;
import cn.academy.Resources;
import cn.academy.block.tileentity.TilePhaseGen;
import cn.lambdalib2.render.obj.ObjLegacyRender;
import cn.lambdalib2.util.RenderUtils;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.render.legacy.TileEntitySpecialRenderer;
import net.minecraft.resources.Identifier;


/**
 * @author WeAthFolD
 */
public class RenderPhaseGen extends TileEntitySpecialRenderer<TilePhaseGen> {
    public static final RenderPhaseGen instance = new RenderPhaseGen();
    
    ObjLegacyRender model;
    Identifier[] textures;
    
    public RenderPhaseGen() {
        model = Resources.getModel("ip_gen");
        textures = Resources.getTextureSeq("models/ip_gen", 5);
    }

    @Override
    public void render(TilePhaseGen gen,
        double x, double y, double z, float partialTicks, int destroyStage, float wtf) {
        GL11.glPushMatrix();
        GL11.glTranslated(x + 0.5, y, z + 0.5);
        int tid = MathUtils.clampi(0, 4, 
            (int) Math.round(4.0 * gen.getLiquidAmount() / gen.getTankSize()));
        RenderUtils.loadTexture(textures[tid]);
        model.renderAll();
        GL11.glPopMatrix();
    }

}