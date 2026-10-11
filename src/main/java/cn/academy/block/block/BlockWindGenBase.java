package cn.academy.block.block;

import cn.academy.ACItems;
import cn.academy.block.container.ContainerWindGenBase;
import cn.academy.block.tileentity.TileWindGenBase;
import cn.academy.energy.client.ui.GuiWindGenBase;
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
public class BlockWindGenBase extends ACBlockMulti {

    @RegGuiHandler
    public static GuiHandlerBase guiHandler = new GuiHandlerBase() {
        @Environment(EnvType.CLIENT)
        @Override
        protected Screen getClientScreen(AbstractContainerMenu container, Player player) {
            return GuiWindGenBase.apply((ContainerWindGenBase) container);
        }

        @Override
        protected AbstractContainerMenu getServerContainer(int id, Player player, Level world, BlockPos pos) {
            if (!(world.getBlockState(pos).getBlock() instanceof BlockWindGenBase block))
                return null;
            BlockEntity te = block.getOriginTile(world, pos);
            return te instanceof TileWindGenBase tile ? new ContainerWindGenBase(this, id, player, tile) : null;
        }
    };

    public BlockWindGenBase(Properties props) {
        super(props);
        addSubBlock(new int[][] {
            { 0, 1, 0 }
        });
        finishInit();
    }

    @Override
    public BlockEntity createNewTileEntity(BlockPos pos, BlockState state) {
        return new TileWindGenBase(pos, state);
    }

    @Override
    public double[] getRotCenter() {
        return new double[] { 0.5, 0, 0.5 };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if(player.getMainHandItem().getItem() == ACItems.item_windgen_pillar)
            return InteractionResult.PASS;

        if(!player.isShiftKeyDown()) {
            if (!world.isClientSide()) {
                guiHandler.openGuiContainer(player, world, pos);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

}
