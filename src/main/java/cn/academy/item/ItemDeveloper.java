package cn.academy.item;

import cn.academy.ability.client.ui.DeveloperUI;
import cn.academy.ability.develop.DeveloperType;
import cn.academy.ability.develop.PortableDevData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The portable developer.
 * @author WeAthFolD
 */
public class ItemDeveloper extends ItemEnergyBase {

    public static final DeveloperType type = DeveloperType.PORTABLE;

    public ItemDeveloper(Properties props) {
        super(type.getEnergy(), type.getBandwidth(), props);
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        if(world.isClientSide()) {
            displayGui(player);
        }
        return InteractionResult.SUCCESS;
    }

    @Environment(EnvType.CLIENT)
    private static void displayGui(Player player) {
        Minecraft.getInstance().gui.setScreen(DeveloperUI.apply(PortableDevData.get(player)));
    }

    @Override
    public double getMaxEnergy() {
        return type.getEnergy();
    }

    @Override
    public double getBandwidth() {
        return type.getBandwidth();
    }

}
