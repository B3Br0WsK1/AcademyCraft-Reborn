package cn.academy.block.container;
import cn.academy.crafting.MetalFormerRecipes;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;


/**
 * @author KSkun
 */
public class SlotMFItem extends Slot {

    private int slot;

    public SlotMFItem(Container inv, int _slot, int x, int y) {
        super(inv, _slot, x, y);
        slot = _slot;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if(slot == 0) {
            for (MetalFormerRecipes.RecipeObject obj : MetalFormerRecipes.INSTANCE.getAllRecipes()) {
                if (obj.input.getItem() == stack.getItem()) return true;
            }
        }
        return false;
    }

}