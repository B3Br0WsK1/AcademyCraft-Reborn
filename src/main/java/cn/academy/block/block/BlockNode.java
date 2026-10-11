package cn.academy.block.block;

import cn.academy.block.container.ContainerNode;
import cn.academy.block.tileentity.TileNode;
import cn.academy.energy.client.ui.GuiNode;
import cn.lambdalib2.registry.mc.gui.GuiHandlerBase;
import cn.lambdalib2.registry.mc.gui.RegGuiHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jspecify.annotations.Nullable;

/**
 * Wireless Node block.
 * @author WeathFolD
 */
public class BlockNode extends ACBlockContainer {

    public static final BooleanProperty CONNECTED = BooleanProperty.create("connected");

    public static final IntegerProperty ENERGY = IntegerProperty.create("energy", 0, 4);

    public enum NodeType implements StringRepresentable {
        BASIC("basic", 15000, 150, 9, 5),
        STANDARD("standard", 50000, 300, 12, 10),
        ADVANCED("advanced", 200000, 900, 19, 20);

        public final int maxEnergy, bandwidth, range, capacity;
        public final String name;
        NodeType(String _name, int _maxEnergy, int _bandwidth, int _range, int _capacity) {
            name = _name;
            maxEnergy = _maxEnergy;
            bandwidth = _bandwidth;
            range = _range;
            capacity = _capacity;
        }

        public String getName() {
            return name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public final NodeType type;

    public BlockNode(NodeType _type, Properties props) {
        super(props, guiHandler);
        type = _type;
        registerDefaultState(stateDefinition.any().setValue(CONNECTED, false).setValue(ENERGY, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONNECTED, ENERGY);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        if (placer instanceof Player player) {
            BlockEntity tile = world.getBlockEntity(pos);
            if (tile instanceof TileNode node) {
                node.setPlacer(player);
            }
        }
    }

    @Override
    public BlockEntity createNewTileEntity(BlockPos pos, BlockState state) {
        return new TileNode(pos, state);
    }

    @RegGuiHandler
    public static GuiHandlerBase guiHandler = new GuiHandlerBase() {
        @Override
        @Environment(EnvType.CLIENT)
        protected Screen getClientScreen(AbstractContainerMenu container, Player player) {
            return GuiNode.apply((ContainerNode) container);
        }

        @Override
        protected AbstractContainerMenu getServerContainer(int id, Player player, Level world, BlockPos pos) {
            BlockEntity te = world.getBlockEntity(pos);
            return te instanceof TileNode node ? new ContainerNode(this, id, node, player) : null;
        }
    };

}
