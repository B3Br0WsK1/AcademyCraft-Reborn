package cn.academy.block.container;

import cn.academy.ACItems;
import cn.academy.block.tileentity.TileImagFusor;
import cn.academy.crafting.ImagFusorRecipes;
import cn.academy.energy.api.IFItemManager;
import cn.academy.item.ItemMatterUnit;
import cn.lambdalib2.registry.mc.gui.GuiHandlerBase;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import static cn.academy.block.tileentity.TileImagFusor.*;

/**
 * @author WeAthFolD
 *
 */
public class ContainerImagFusor extends TechUIContainer<TileImagFusor> {

    public ContainerImagFusor(GuiHandlerBase handler, int id, TileImagFusor _tile, Player _player) {
        super(handler, id, _player, _tile);

        initInventory();
    }

    private void initInventory() {
        this.addSlotToContainer(new SlotCrystal(tile, SLOT_INPUT, 13, 49));
        this.addSlotToContainer(new SlotCrystal(tile, SLOT_OUTPUT, 143, 49));
        this.addSlotToContainer(new SlotMatterUnit(tile, ItemMatterUnit.MAT_PHASE_LIQUID, SLOT_IMAG_INPUT, 13, 10));
        this.addSlotToContainer(new SlotMatterUnit(tile, ItemMatterUnit.MAT_PHASE_LIQUID, SLOT_IMAG_OUTPUT, 143, 10));
        this.addSlotToContainer(new SlotIFItem(tile, SLOT_ENERGY_INPUT, 42, 80));

        mapPlayerInventory();

        ItemMatterUnit unit = ACItems.matter_unit;
        SlotGroup inventoryGroup = gRange(5, slots.size());

        this.addTransferRule(inventoryGroup,
                stack -> unit.getMaterial(stack) == ItemMatterUnit.MAT_PHASE_LIQUID,
                gSlots(2));

        this.addTransferRule(inventoryGroup,
                stack -> IFItemManager.instance.isSupported(stack),
                gSlots(4));

        this.addTransferRule(inventoryGroup, gSlots(0));

        this.addTransferRule(gRange(0, 5), inventoryGroup);
    }

    /**
     * @author KSkun
     */
    private static class SlotCrystal extends Slot {

        private int slot;

        public SlotCrystal(Container inv, int _slot, int x, int y) {
            super(inv, _slot, x, y);
            slot = _slot;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if(slot == 0) {
                for (ImagFusorRecipes.IFRecipe obj : ImagFusorRecipes.INSTANCE.getAllRecipe()) {
                    if (obj.consumeType.getItem() == stack.getItem()) return true;
                }
            }
            return false;
        }

    }
}
