package cn.lambdalib2.render.legacy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1.12 TileEntitySpecialRenderer compatibility class. Override
 *  {@link #render(BlockEntity, double, double, double, float, int, float)}.
 */
@Environment(EnvType.CLIENT)
public abstract class TileEntitySpecialRenderer<T extends BlockEntity> extends LegacyTileRenderer<T> {

    @Override
    public final void render(T te, double x, double y, double z, float partialTicks) {
        render(te, x, y, z, partialTicks, -1, 1.0f);
    }

    /**
     * Renders the block entity, (x, y, z) being its camera relative position.
     */
    public void render(T te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {}

}
