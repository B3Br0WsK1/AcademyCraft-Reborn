package cn.academy.item;

import cn.academy.advancements.ACAdvancements;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.NetworkS11nType;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * @author WeAthFolD
 */
@NetworkS11nType
public class ItemTutorial extends Item {

    private static final String MSG_TRIGGER = ".item.tutorial.trigger_advance";

    public ItemTutorial(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        if(world.isClientSide()) {
            openGui();
        } else {
            ACAdvancements.trigger(player, ACAdvancements.open_misaka_cloud.ID);
        }
        return InteractionResult.SUCCESS;
    }

    @Environment(EnvType.CLIENT)
    private static void openGui() {
        Minecraft.getInstance().gui.setScreen(new cn.academy.client.gui.GuiTutorial());
    }

}
