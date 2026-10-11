package cn.academy.block.block;

import cn.academy.block.container.ContainerImagFusor;
import cn.academy.block.tileentity.TileImagFusor;
import cn.academy.crafting.client.ui.GuiImagFusor;
import cn.lambdalib2.registry.mc.gui.GuiHandlerBase;
import cn.lambdalib2.registry.mc.gui.RegGuiHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * The Imag Fusor. FRAME is 0 when idle, 1~4 cycles while working (updated by the tile).
 * @author WeAthFolD
 */
public class BlockImagFusor extends ACBlockContainer {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty FRAME = IntegerProperty.create("frame", 0, 4);

    public BlockImagFusor(Properties props) {
        super(props, guiHandler);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FRAME, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FRAME);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockEntity createNewTileEntity(BlockPos pos, BlockState state) {
        return new TileImagFusor(pos, state);
    }

    @RegGuiHandler
    public static GuiHandlerBase guiHandler = new GuiHandlerBase() {
        @Environment(EnvType.CLIENT)
        @Override
        protected Screen getClientScreen(AbstractContainerMenu container, Player player) {
            return GuiImagFusor.apply((ContainerImagFusor) container);
        }

        @Override
        protected AbstractContainerMenu getServerContainer(int id, Player player, Level world, BlockPos pos) {
            BlockEntity te = world.getBlockEntity(pos);
            return te instanceof TileImagFusor fusor ? new ContainerImagFusor(this, id, fusor, player) : null;
        }

    };

}
