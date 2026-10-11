package cn.academy.block.block;

import cn.academy.block.container.ContainerMatrix;
import cn.academy.block.tileentity.TileMatrix;
import cn.academy.energy.client.ui.GuiMatrix2;
import cn.lambdalib2.registry.mc.gui.GuiHandlerBase;
import cn.lambdalib2.registry.mc.gui.RegGuiHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * @author WeAthFolD
 *
 */
public class BlockMatrix extends ACBlockMulti {

    public BlockMatrix(Properties props) {
        super(props);

        addSubBlock(0, 0, 1);
        addSubBlock(1, 0, 1);
        addSubBlock(1, 0, 0);

        addSubBlock(0, 1, 0);
        addSubBlock(0, 1, 1);
        addSubBlock(1, 1, 1);
        addSubBlock(1, 1, 0);

        this.finishInit();
    }

    @Override
    public BlockEntity createNewTileEntity(BlockPos pos, BlockState state) {
        return new TileMatrix(pos, state);
    }

    @Override
    public double[] getRotCenter() {
        return new double[] { 1.0, 0, 1.0};
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        // There is a small delay before tileEntity is set after placement
        BlockPos center = this.getOrigin(world, pos);
        if (center == null)
            return InteractionResult.PASS;

        if(!player.isShiftKeyDown()) {
            guiHandler.openGuiContainer(player, world, center);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (placer instanceof Player player) {
            BlockEntity tile = world.getBlockEntity(pos);
            if (tile instanceof TileMatrix matrix) {
                matrix.setPlacer(player);
            }
        }

        super.setPlacedBy(world, pos, state, placer, stack);
    }

    @RegGuiHandler
    public static GuiHandlerBase guiHandler = new GuiHandlerBase() {
        @Environment(EnvType.CLIENT)
        @Override
        protected Screen getClientScreen(AbstractContainerMenu container, Player player) {
            return GuiMatrix2.apply((ContainerMatrix) container);
        }

        @Override
        protected AbstractContainerMenu getServerContainer(int id, Player player, Level world, BlockPos pos) {
            BlockEntity te = world.getBlockEntity(pos);
            return te instanceof TileMatrix matrix ? new ContainerMatrix(this, id, matrix, player) : null;
        }
    };

}
