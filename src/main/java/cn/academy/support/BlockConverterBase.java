package cn.academy.support;
import cn.academy.block.block.ACBlockContainer;
import cn.academy.core.client.ui.WirelessPage;
import cn.academy.energy.api.block.IWirelessUser;
import cn.academy.util.ACI18n;
import cn.lambdalib2.cgui.CGuiScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Consumer;

/**
 * Base of the energy converter blocks (used by the energy-mod support modules).
 * @author WeAthFolD
 */
public abstract class BlockConverterBase extends ACBlockContainer {

    public static class Item extends BlockItem {

        final BlockConverterBase converter;

        public Item(Block block, Properties props) {
            super(block, props);
            converter = (BlockConverterBase) block;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                    Consumer<Component> builder, TooltipFlag flag) {
            builder.accept(Component.literal(ACI18n.translateFormatted("ac.converter.desc_template",
                converter.from, converter.to)));
        }

    }

    /**
     * Default block properties of a converter (stone-like, mineable with a pickaxe via the block tags).
     */
    public static Properties defaultProperties(Properties props) {
        return props.strength(2.5f).sound(SoundType.STONE).requiresCorrectToolForDrops();
    }

    public final Class<? extends BlockEntity> tileType;
    public final String from, to;

    public BlockConverterBase(Properties props, String _from, String _to, Class<? extends BlockEntity> _tileType) {
        super(defaultProperties(props));
        from = _from;
        to = _to;
        tileType = _tileType;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te != null && tileType.isInstance(te)) {
            if (te instanceof IWirelessUser user && !player.isShiftKeyDown()) {
                if (world.isClientSide()) {
                    displayGui(user);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Environment(EnvType.CLIENT)
    private void displayGui(IWirelessUser te) {
        CGuiScreen screen = new CGuiScreen() {
            @Override
            public boolean isPauseScreen() {
                return false;
            }
        };
        screen.getGui().addWidget(WirelessPage.userPage((BlockEntity) te).window());

        Minecraft.getInstance().gui.setScreen(screen);
    }

}
