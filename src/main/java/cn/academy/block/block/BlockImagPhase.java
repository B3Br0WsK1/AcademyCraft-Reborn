package cn.academy.block.block;

import cn.academy.block.tileentity.TileImagPhase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import org.jspecify.annotations.Nullable;

/**
 * Imaginary phase liquid block. Has a block entity only for its special layered rendering.
 * Matter units pick it up (see ItemMatterUnit).
 * @author WeAthFolD
 */
public class BlockImagPhase extends LiquidBlock implements EntityBlock {

    public BlockImagPhase(FlowingFluid fluid, Properties props) {
        super(fluid, props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileImagPhase(pos, state);
    }

}
