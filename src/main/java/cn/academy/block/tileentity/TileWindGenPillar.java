package cn.academy.block.tileentity;

import cn.academy.ACTiles;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * @author WeAthFolD
 */
public class TileWindGenPillar extends BlockEntity {

    public TileWindGenPillar(BlockPos pos, BlockState state) {
        super(ACTiles.WINDGEN_PILLAR, pos, state);
    }

}
