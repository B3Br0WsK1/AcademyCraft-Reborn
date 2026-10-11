package cn.academy.client.render.block;
import cn.lambdalib2.render.legacy.GL11;
import cn.academy.Resources;
import cn.academy.block.tileentity.TileImagPhase;
import cn.lambdalib2.util.RenderUtils;
import cn.lambdalib2.util.GameTimer;
import net.minecraft.client.Minecraft;
import cn.lambdalib2.render.legacy.OpenGlHelper;
import cn.lambdalib2.render.legacy.RenderHelper;
import cn.lambdalib2.render.legacy.Tessellator;
import cn.lambdalib2.render.legacy.TileEntitySpecialRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;



/**
 * @author WeAthFolD
 */
public class RenderImagPhaseLiquid extends TileEntitySpecialRenderer<BlockEntity> {

    @Override
    protected boolean useLighting() {
        return false;
    }

    private Identifier[] layers;

    private Tessellator t;


    public RenderImagPhaseLiquid() {
        t = Tessellator.instance;
        layers = Resources.getEffectSeq("imag_proj_liquid", 3);

    }

    private float getFluidHeight(net.minecraft.world.level.Level level, BlockPos pos) {
        return level.getFluidState(pos).getHeight(level, pos);
    }

    @Override
    public void render(BlockEntity te, double x,
            double y, double z, float w, int destroyStage, float alpha_) {
        if(!(te.getBlockState().getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) || te.getLevel() == null)
            return;

        BlockPos p = te.getBlockPos();
        double distSq = Minecraft.getInstance().player.distanceToSqr(p.getX() + .5, p.getY() + .5, p.getZ() + .5);
        double alpha = 1 / (1 + 0.2 * Math.pow(distSq, 0.5));
        
        if(alpha < 1E-1)
            return;
        
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        
        GL11.glColor4d(1, 1, 1, alpha);

        RenderHelper.disableStandardItemLighting();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.defaultTexUnit, 240f, 240f);
        double ht = 1.2 * Math.sqrt(
            getFluidHeight(te.getLevel(), p)
        );

        GL11.glEnable(GL11.GL_BLEND);
        drawLayer(0, -0.3 * ht , 0.3, 0.2, 0.7);
        drawLayer(1, 0.35 * ht, 0.3, 0.05, 0.7);
        if(ht > 0.5)
            drawLayer(2, 0.7 * ht, 0.1, 0.25, 0.7);

        RenderHelper.enableStandardItemLighting();
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        
        GL11.glPopMatrix();
    }
    
    private void drawLayer(int layer, double height, double vx, double vz, double density) {
        double time = GameTimer.getTime();
        double du = (time * vx) % 1;
        double dv = (time * vz) % 1;
        
        RenderUtils.loadTexture(layers[layer]);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
        t.startDrawingQuads();
        t.addVertexWithUV(0, height, 0, du, dv);
        t.addVertexWithUV(1, height, 0, du + density, dv);
        t.addVertexWithUV(1, height, 1, du + density, dv + density);
        t.addVertexWithUV(0, height, 1, du, dv + density);
        t.draw();
    }
    
}