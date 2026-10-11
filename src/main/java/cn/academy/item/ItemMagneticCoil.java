package cn.academy.item;

import cn.academy.util.ACI18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;

public class ItemMagneticCoil extends Item {

    public ItemMagneticCoil(Properties props) {
        super(props);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> builder, TooltipFlag flag) {
        for (String line : ACI18n.translate("item.ac_magnetic_coil.desc").split("<br>")) {
            builder.accept(Component.literal(line));
        }
    }

}
