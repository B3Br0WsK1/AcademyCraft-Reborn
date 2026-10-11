package cn.academy.client.render.block;
import cn.lambdalib2.render.legacy.GL11;
import net.minecraft.world.level.block.Block;
import net.minecraft.client.Minecraft;
import cn.lambdalib2.render.legacy.RenderHelper;
import cn.lambdalib2.render.legacy.Tessellator;
import cn.lambdalib2.render.legacy.TileEntitySpecialRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;


/**
 * Vanilla mc block rendering pipeline caches block rendering. If you want to render some block
 * that renders dynamically and wants to use the original IIcon approach, use this on your BlockEntity.
 * @author WeAthFolD
 */
@Environment(EnvType.CLIENT)
public class RenderDynamicBlock extends TileEntitySpecialRenderer {

    // TODO figure out how to render blocks properly
//    public static RenderBlocks renderBlocks = RenderBlocks.getInstance();
//
//    @Override
//    public void renderTileEntityAt(BlockEntity t, double x,
//            double y, double z, float pt) {
//        Block blockType = t.getBlockType();
//
//        Tessellator tes = Tessellator.instance;
//        tes.setColorOpaque_F(1.0F, 1.0F, 1.0F);
//
//        renderBlocks.blockAccess = t.getWorld();
//
//        {
//            if (Minecraft.isAmbientOcclusionEnabled()) {
//                GL11.glShadeModel(GL11.GL_SMOOTH);
//            } else {
//                GL11.glShadeModel(GL11.GL_FLAT);
//            }
//            RenderHelper.disableStandardItemLighting();
//
//            RenderUtils.loadTexture(TextureMap.locationBlocksTexture);
//
//            tes.startDrawingQuads();
//            tes.setTranslation(x - t.x, y - t.y, z - t.z);
//
//            renderBlocks.renderBlockAllFaces(blockType, t.x, t.y, t.z);
//            tes.setTranslation(0, 0, 0);
//            tes.draw();
//
//            RenderHelper.enableStandardItemLighting();
//        }
//    }

}