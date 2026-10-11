package cn.academy.block.container;

import cn.academy.ACItems;
import cn.academy.block.tileentity.TilePhaseGen;
import cn.academy.energy.api.IFItemManager;
import cn.academy.item.ItemMatterUnit;
import cn.lambdalib2.registry.mc.gui.GuiHandlerBase;
import net.minecraft.world.entity.player.Player;

/**
 * @author WeAthFolD
 */
public class ContainerPhaseGen extends TechUIContainer<TilePhaseGen> {

    public static final int SLOT_LIQUID_IN = 0, SLOT_LIQUID_OUT = 1, SLOT_OUTPUT = 2;

    public ContainerPhaseGen(GuiHandlerBase handler, int id, Player _player, TilePhaseGen _tile) {
        super(handler, id, _player, _tile);

        initInventory();
    }

    private void initInventory() {
        this.addSlotToContainer(new SlotMatterUnit(tile, ItemMatterUnit.MAT_PHASE_LIQUID, SLOT_LIQUID_IN, 45, 12));
        this.addSlotToContainer(new SlotMatterUnit(tile, ItemMatterUnit.MAT_PHASE_LIQUID, SLOT_LIQUID_OUT, 112, 51));
        this.addSlotToContainer(new SlotIFItem(tile, SLOT_OUTPUT, 42, 80));

        mapPlayerInventory();

        SlotGroup gMachine = gRange(0, 3);
        SlotGroup gInv = gRange(3, 3+36);

        addTransferRule(gMachine, gInv);
        addTransferRule(gInv, IFItemManager.instance::isSupported, gSlots(SLOT_OUTPUT));
        addTransferRule(gInv, stack -> ACItems.matter_unit.getMaterial(stack) == ItemMatterUnit.MAT_PHASE_LIQUID, gSlots(SLOT_LIQUID_IN));
    }

}
