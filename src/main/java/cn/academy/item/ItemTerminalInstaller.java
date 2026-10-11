package cn.academy.item;

import cn.academy.advancements.ACAdvancements;
import cn.academy.client.auxgui.TerminalInstallEffect;
import cn.academy.terminal.TerminalData;
import cn.lambdalib2.auxgui.AuxGuiHandler;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.NetworkS11nType;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * @author WeAthFolD
 */
@NetworkS11nType
public class ItemTerminalInstaller extends Item  {

    public ItemTerminalInstaller(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        TerminalData tData = TerminalData.get(player);
        if(tData.isTerminalInstalled()) {
            if(!world.isClientSide())
                player.sendSystemMessage(Component.translatable("ac.terminal.alrdy_installed"));
        } else {
            if(!world.isClientSide()) {
                if(!player.getAbilities().instabuild)
                    stack.shrink(1);
                tData.install();
                ACAdvancements.trigger(player, ACAdvancements.terminal_installed.ID);
                NetworkMessage.sendTo(player, NetworkMessage.staticCaller(ItemTerminalInstaller.class), "install");
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Environment(EnvType.CLIENT)
    @Listener(channel="install", side=Side.CLIENT)
    private static void install() {
        AuxGuiHandler.register(new TerminalInstallEffect());
    }

}
