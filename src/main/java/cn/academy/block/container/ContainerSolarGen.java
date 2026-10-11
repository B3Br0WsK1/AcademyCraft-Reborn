package cn.academy.block.container;

import cn.academy.block.tileentity.TileSolarGen;
import cn.academy.energy.api.IFItemManager;
import cn.lambdalib2.registry.mc.gui.GuiHandlerBase;
import net.minecraft.world.entity.player.Player;

public class ContainerSolarGen extends TechUIContainer<TileSolarGen> {

    public ContainerSolarGen(GuiHandlerBase handler, int id, Player player, TileSolarGen tile) {
        super(handler, id, player, tile);

        this.addSlotToContainer(new SlotIFItem(tile, TileSolarGen.SLOT_BATTERY, 42, 81));

        mapPlayerInventory();

        SlotGroup gInv = gRange(1, 1+36);
        SlotGroup gBattergy = gSlots(0);

        addTransferRule(gInv, IFItemManager.instance::isSupported, gBattergy);
        addTransferRule(gBattergy, gInv);
    }

}
