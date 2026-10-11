package cn.academy.block.container;

import cn.academy.block.tileentity.TileAbilityInterferer;
import cn.academy.support.EnergyItemHelper;
import cn.lambdalib2.registry.mc.gui.GuiHandlerBase;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Created by Paindar on 2017/3/31.
 */
public class ContainAbilityInterferer extends TechUIContainer<TileAbilityInterferer>
{
    static class SlotAIItem extends Slot
    {
        public SlotAIItem(Container inv, int _slot, int x, int y) {
            super(inv, _slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack)
        {
            return EnergyItemHelper.isSupported(stack);
        }

    }

    public ContainAbilityInterferer(GuiHandlerBase handler, int id, TileAbilityInterferer _tile, Player _player) {
        super(handler, id, _player, _tile);

        initInventory();
    }

    private void initInventory() {
        this.addSlotToContainer(new SlotAIItem(tile, 0, 139, 25));
        Inventory inv = player.getInventory();

        for(int i = 0; i < 9; ++i) {
            addSlotToContainer(new Slot(inv, i, 6 + i*18, 163));
        }

        SlotGroup gInv = gRange(1, 10), gMachine = gSlots(0);

        addTransferRule(gMachine, gInv);
        addTransferRule(gInv, EnergyItemHelper::isSupported, gSlots(TileAbilityInterferer.SLOT_BATTERY));
    }

}
