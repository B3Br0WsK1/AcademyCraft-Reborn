package cn.academy.block.container;

import cn.academy.ACItems;
import cn.academy.block.tileentity.TileMatrix;
import cn.lambdalib2.registry.mc.gui.GuiHandlerBase;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * @author WeAthFolD
 */
public class ContainerMatrix extends TechUIContainer<TileMatrix> {

    public ContainerMatrix(GuiHandlerBase handler, int id, TileMatrix _tile, Player _player) {
        super(handler, id, _player, _tile);

        initInventory();
    }

    private void initInventory() {
        this.addSlotToContainer(new SlotPlate(tile, 0, 78, 11));
        this.addSlotToContainer(new SlotPlate(tile, 1, 53, 60));
        this.addSlotToContainer(new SlotPlate(tile, 2, 104, 60));

        this.addSlotToContainer(new SlotCore(tile, 3, 78, 36));

        mapPlayerInventory();

        SlotGroup invGroup = gRange(4, 4 + 36);

        addTransferRule(invGroup, stack -> stack.getItem() == ACItems.constraint_plate, gSlots(0, 1, 2));
        addTransferRule(invGroup, TileMatrix::isMatrixCore, gSlots(3));
        addTransferRule(gRange(0, 4), invGroup);
    }

    public static class SlotCore extends Slot {

        public SlotCore(Container inv, int slot, int x, int y) {
            super(inv, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return TileMatrix.isMatrixCore(stack);
        }

    }

    public static class SlotPlate extends Slot {

        public SlotPlate(Container inv, int slot, int x, int y) {
            super(inv, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !stack.isEmpty() && stack.getItem() == ACItems.constraint_plate;
        }

    }

}
