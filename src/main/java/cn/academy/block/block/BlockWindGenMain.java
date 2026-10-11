package cn.academy.block.block;

import cn.academy.block.container.ContainerWindGenMain;
import cn.academy.block.tileentity.TileWindGenMain;
import cn.academy.energy.client.ui.GuiWindGenMain;
import cn.lambdalib2.registry.mc.gui.GuiHandlerBase;
import cn.lambdalib2.registry.mc.gui.RegGuiHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * @author WeAthFolD
 */
public class BlockWindGenMain extends ACBlockMulti {

    @RegGuiHandler
    public static GuiHandlerBase guiHandler = new GuiHandlerBase() {
        @Environment(EnvType.CLIENT)
        @Override
        protected Screen getClientScreen(AbstractContainerMenu container, Player player) {
            return GuiWindGenMain.apply((ContainerWindGenMain) container);
        }

        @Override
        protected AbstractContainerMenu getServerContainer(int id, Player player, Level world, BlockPos pos) {
            if (!(world.getBlockState(pos).getBlock() instanceof BlockWindGenMain block))
                return null;
            BlockEntity te = block.getOriginTile(world, pos);
            return te instanceof TileWindGenMain gen ? new ContainerWindGenMain(this, id, player, gen) : null;
        }
    };

    public BlockWindGenMain(Properties props) {
        super(props);
        this.addSubBlock(new int[][] {
            { 0, 0, -1 },
            { 0, 0, 1 }
        });
        finishInit();
    }

    @Override
    public BlockEntity createNewTileEntity(BlockPos pos, BlockState state) {
        return new TileWindGenMain(pos, state);
    }

    @Override
    public double[] getRotCenter() {
        return new double[] { 0.5, 0, 0.4 };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if(!player.isShiftKeyDown()) {
            if (!world.isClientSide()) {
                guiHandler.openGuiContainer(player, world, pos);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

}
