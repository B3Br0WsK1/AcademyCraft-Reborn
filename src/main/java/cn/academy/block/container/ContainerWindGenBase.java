package cn.academy.block.container;

import cn.academy.block.tileentity.TileWindGenBase;
import cn.academy.energy.api.IFItemManager;
import cn.lambdalib2.registry.mc.gui.GuiHandlerBase;
import net.minecraft.world.entity.player.Player;

/**
 * @author WeAthFolD
 */
public class ContainerWindGenBase extends TechUIContainer<TileWindGenBase> {

    public ContainerWindGenBase(GuiHandlerBase handler, int id, Player _player, TileWindGenBase _tile) {
        super(handler, id, _player, _tile);

        initInventory();
    }

    private void initInventory() {
        this.addSlotToContainer(new SlotIFItem(tile, 0, 42, 80));

        mapPlayerInventory();

        SlotGroup gInv = gRange(1, 1+36), gBattery = gSlots(0);

        addTransferRule(gBattery, gInv);
        addTransferRule(gInv, IFItemManager.instance::isSupported, gBattery);
    }

}
