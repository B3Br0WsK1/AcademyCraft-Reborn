package cn.academy.block.block;

import cn.academy.block.container.ContainerPhaseGen;
import cn.academy.block.tileentity.TilePhaseGen;
import cn.academy.energy.client.ui.GuiPhaseGen;
import cn.lambdalib2.registry.mc.gui.GuiHandlerBase;
import cn.lambdalib2.registry.mc.gui.RegGuiHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * @author WeAthFolD
 */
public class BlockPhaseGen extends ACBlockContainer {

    @RegGuiHandler
    public static GuiHandlerBase guiHandler = new GuiHandlerBase() {
        @Environment(EnvType.CLIENT)
        @Override
        protected Screen getClientScreen(AbstractContainerMenu container, Player player) {
            return GuiPhaseGen.apply((ContainerPhaseGen) container);
        }

        @Override
        protected AbstractContainerMenu getServerContainer(int id, Player player, Level world, BlockPos pos) {
            BlockEntity te = world.getBlockEntity(pos);
            return te instanceof TilePhaseGen gen ? new ContainerPhaseGen(this, id, player, gen) : null;
        }
    };

    public BlockPhaseGen(Properties props) {
        super(props, guiHandler);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public BlockEntity createNewTileEntity(BlockPos pos, BlockState state) {
        return new TilePhaseGen(pos, state);
    }

}
