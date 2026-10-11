package cn.academy.item;

import cn.academy.energy.api.IFItemManager;
import cn.academy.energy.api.item.ImagEnergyItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;


/**
 * Energy item. The energy level is shown with the item damage (13 steps), which the item model selects on.
 * @author WeAthFolD
 *
 */
public class ItemEnergyBase extends Item implements ImagEnergyItem, ISubItemProvider {

    protected static IFItemManager itemManager = IFItemManager.instance;

    public final double maxEnergy;
    public final double bandwidth;

    public ItemEnergyBase(double _maxEnergy, double _bandwidth, Properties props) {
        super(props);
        maxEnergy = _maxEnergy;
        bandwidth = _bandwidth;
    }

    @Override
    public double getMaxEnergy() {
        return maxEnergy;
    }

    @Override
    public double getBandwidth() {
        return bandwidth;
    }

    @Override
    public void addSubItems(Consumer<ItemStack> items) {
        ItemStack is = new ItemStack(this);
        itemManager.charge(is, 0, true);
        items.accept(is);

        is = new ItemStack(this);
        itemManager.charge(is, Double.MAX_VALUE, true);
        items.accept(is);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.literal(itemManager.getDescription(stack)));
    }

}
