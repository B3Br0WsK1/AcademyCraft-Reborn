package cn.academy.block.container;
import cn.academy.ACItems;
import cn.academy.item.ItemMatterUnit.MatterMaterial;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;


/**
 * @author WeAthFolD
 */
public class SlotMatterUnit extends Slot {
    
    final MatterMaterial material;

    public SlotMatterUnit(Container inv, int slot, int x, int y) {
        this(inv, null, slot, x, y);
    }
    
    public SlotMatterUnit(Container inv, MatterMaterial mat, int slot, int x, int y) {
        super(inv, slot, x, y);
        material = mat;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return (!stack.isEmpty() && 
                stack.getItem() == ACItems.matter_unit &&
                (material == null || ACItems.matter_unit.getMaterial(stack) == material));
    }

}