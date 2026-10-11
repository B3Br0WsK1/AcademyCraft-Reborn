package cn.academy.block.block;

import cn.lambdalib2.multiblock.BlockMulti;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Base class of AcademyCraft multiblocks. The origin block entity is ticked if it's an ITickableTile.
 * @author WeAthFolD
 */
public abstract class ACBlockMulti extends BlockMulti {

    public ACBlockMulti(Properties props) {
        super(props);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return ACBlockContainer.tickerFor();
    }

}
