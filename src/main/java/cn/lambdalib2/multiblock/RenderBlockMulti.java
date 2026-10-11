package cn.lambdalib2.multiblock;

import cn.lambdalib2.render.legacy.GL11;
import cn.lambdalib2.render.legacy.TileEntitySpecialRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The BlockMulti base render, which focuses on placement judging. Concrete
 * render ways belongs to its subclasses.
 *
 * @author WeathFolD
 */
@Environment(EnvType.CLIENT)
public abstract class RenderBlockMulti<T extends BlockEntity> extends TileEntitySpecialRenderer<T> {

    public RenderBlockMulti() {
    }

    @Override
    public void render(T te, double x, double y, double z, float partialTicks,
        int destroyStage, float alpha) {
        if (!(te.getBlockState().getBlock() instanceof BlockMulti bm))
            return;

        InfoBlockMulti inf = InfoBlockMulti.of(te);

        if (inf.getSubID() != 0)
            return;
        Direction dir = inf.getDir();
        GL11.glPushMatrix();
        double[] off = bm.getPivotOffset(dir);
        double[] off2 = bm.getRotCenter(dir);
        GL11.glTranslated(x + off[0] + off2[0], y + 0 + off2[1], z + off[1] + off2[2]);
        GL11.glRotated(bm.getRotationDegrees(dir), 0, 1, 0);
        drawAtOrigin(te);
        GL11.glPopMatrix();
    }

    public abstract void drawAtOrigin(T te);

}
