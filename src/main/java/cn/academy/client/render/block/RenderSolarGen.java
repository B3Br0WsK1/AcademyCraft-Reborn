package cn.academy.client.render.block;
import cn.lambdalib2.render.legacy.GL11;
import cn.academy.Resources;
import cn.academy.block.tileentity.TileSolarGen;
import cn.lambdalib2.multiblock.RenderBlockMulti;
import cn.lambdalib2.render.obj.ObjLegacyRender;
import cn.lambdalib2.util.RenderUtils;
import net.minecraft.resources.Identifier;


/**
 * @author WeAthFolD
 */
public class RenderSolarGen extends RenderBlockMulti<TileSolarGen> {
    public static RenderSolarGen instance = new RenderSolarGen();

    private final float SCALE = 0.014f;

    private ObjLegacyRender _mdl = Resources.getModel("solar");

    private Identifier _texture = Resources.getTexture("models/solar");

    @Override
    public void drawAtOrigin(TileSolarGen te) {
        GL11.glRotated(90, 0, 1, 0);
        GL11.glScaled(SCALE, SCALE, SCALE);
        RenderUtils.loadTexture(_texture);
        _mdl.renderAll();
    }

}