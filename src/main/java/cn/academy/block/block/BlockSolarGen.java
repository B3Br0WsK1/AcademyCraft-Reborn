package cn.academy.block.block;

import cn.academy.block.container.ContainerSolarGen;
import cn.academy.block.tileentity.TileSolarGen;
import cn.academy.energy.client.ui.GuiSolarGen;
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
public class BlockSolarGen extends ACBlockMulti {

    @RegGuiHandler
    public static final GuiHandlerBase handler = new GuiHandlerBase() {
        @Environment(EnvType.CLIENT)
        @Override
        protected Screen getClientScreen(AbstractContainerMenu container, Player player) {
            return GuiSolarGen.apply((ContainerSolarGen) container);
        }

        @Override
        protected AbstractContainerMenu getServerContainer(int id, Player player, Level world, BlockPos pos) {
            BlockEntity te = world.getBlockEntity(pos);
            return te instanceof TileSolarGen gen ? new ContainerSolarGen(this, id, player, gen) : null;
        }
    };

    public BlockSolarGen(Properties props) {
        super(props);
        this.finishInit();
    }

    @Override
    public BlockEntity createNewTileEntity(BlockPos pos, BlockState state) {
        return new TileSolarGen(pos, state);
    }

    @Override
    public double[] getRotCenter() {
        return new double[] { 0.5, 0, 0.5 };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if(!player.isShiftKeyDown()) {
            if(!world.isClientSide())
                handler.openGuiContainer(player, world, pos);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

}
