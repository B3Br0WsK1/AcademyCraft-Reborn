package cn.academy.block.tileentity;

import cn.academy.ACTiles;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Exists only so that the phase liquid can be rendered with its animated layers.
 */
public class TileImagPhase extends BlockEntity {

    public TileImagPhase(BlockPos pos, BlockState state) {
        super(ACTiles.IMAG_PHASE, pos, state);
    }

}
