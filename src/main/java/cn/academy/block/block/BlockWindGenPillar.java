package cn.academy.block.block;

import cn.academy.block.tileentity.TileWindGenPillar;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * @author WeAthFolD
 */
public class BlockWindGenPillar extends ACBlockContainer {

    public BlockWindGenPillar(Properties props) {
        super(props, null);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public BlockEntity createNewTileEntity(BlockPos pos, BlockState state) {
        return new TileWindGenPillar(pos, state);
    }

}
