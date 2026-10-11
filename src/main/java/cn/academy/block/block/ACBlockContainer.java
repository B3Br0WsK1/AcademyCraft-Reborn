package cn.academy.block.block;

import cn.lambdalib2.registry.mc.gui.GuiHandlerBase;
import cn.lambdalib2.template.tile.ITickableTile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * BaseClass for typical block containers. will automatically try to open the container gui.
 *  Block entities implementing {@link ITickableTile} are ticked on both sides.
 * @author WeAthFolD
 */
public abstract class ACBlockContainer extends BaseEntityBlock {

    final GuiHandlerBase guiHandler;

    public ACBlockContainer(Properties props) {
        this(props, null);
    }

    public ACBlockContainer(Properties props, GuiHandlerBase _guiHandler) {
        super(props);
        guiHandler = _guiHandler;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if(guiHandler != null && !player.isShiftKeyDown()) {
            if(!world.isClientSide())
                guiHandler.openGuiContainer(player, world, pos);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * Creates the block entity of this block.
     */
    public abstract BlockEntity createNewTileEntity(BlockPos pos, BlockState state);

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return createNewTileEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return tickerFor();
    }

    @SuppressWarnings("unchecked")
    static <T extends BlockEntity> BlockEntityTicker<T> tickerFor() {
        return (BlockEntityTicker<T>) TICKER;
    }

    static final BlockEntityTicker<BlockEntity> TICKER = (level, pos, state, be) -> {
        if (be instanceof ITickableTile t) {
            t.update();
        }
    };

}
