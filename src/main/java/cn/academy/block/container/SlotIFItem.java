package cn.academy.block.container;
import cn.academy.energy.api.IFItemManager;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;


/**
 * @author WeAthFolD
 *
 */
public class SlotIFItem extends Slot {

    public SlotIFItem(Container inv, int slot, int x, int y) {
        super(inv, slot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return (!stack.isEmpty() && IFItemManager.instance.isSupported(stack));
    }

}