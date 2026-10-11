package cn.academy.block.block;

import cn.academy.block.container.ContainAbilityInterferer;
import cn.academy.block.tileentity.TileAbilityInterferer;
import cn.academy.crafting.client.ui.GuiAbilityInterferer;
import cn.lambdalib2.registry.mc.gui.GuiHandlerBase;
import cn.lambdalib2.registry.mc.gui.RegGuiHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.Nullable;

/**
 * Created by Paindar on 2017/3/31.
 */
public class BlockAbilityInterferer extends ACBlockContainer
{

    public static final BooleanProperty PROP_ON = BooleanProperty.create("on");

    @RegGuiHandler
    public static GuiHandlerBase guiHandler = new GuiHandlerBase() {
        @Environment(EnvType.CLIENT)
        @Override
        protected Screen getClientScreen(AbstractContainerMenu container, Player player) {
            return GuiAbilityInterferer.apply((ContainAbilityInterferer) container);
        }

        @Override
        protected AbstractContainerMenu getServerContainer(int id, Player player, Level world, BlockPos pos) {
            BlockEntity tile = world.getBlockEntity(pos);
            return tile instanceof TileAbilityInterferer interf ? new ContainAbilityInterferer(this, id, interf, player) : null;
        }
    };

    public BlockAbilityInterferer(Properties props) {
        super(props, guiHandler);
        registerDefaultState(stateDefinition.any().setValue(PROP_ON, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PROP_ON);
    }

    @Override
    public BlockEntity createNewTileEntity(BlockPos pos, BlockState state) {
        return new TileAbilityInterferer(pos, state);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        BlockEntity tile = world.getBlockEntity(pos);
        if(placer instanceof Player player && tile instanceof TileAbilityInterferer interf) {
            interf.setPlacer(player);
        }
    }

}
