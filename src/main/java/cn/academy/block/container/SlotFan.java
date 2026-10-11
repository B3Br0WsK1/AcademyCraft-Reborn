package cn.academy.block.container;
import cn.academy.ACItems;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;


/**
 * @author WeAthFolD
 */
public class SlotFan extends Slot {

    public SlotFan(Container p_i1824_1_, int p_i1824_2_, int p_i1824_3_,
            int p_i1824_4_) {
        super(p_i1824_1_, p_i1824_2_, p_i1824_3_, p_i1824_4_);
    }
    
    @Override
    public boolean mayPlace(ItemStack stack) {
        return (!stack.isEmpty() && stack.getItem() == ACItems.windgen_fan);
    }

}