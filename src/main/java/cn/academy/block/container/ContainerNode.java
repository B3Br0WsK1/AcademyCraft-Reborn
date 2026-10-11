package cn.academy.block.container;

import cn.academy.block.tileentity.TileNode;
import cn.academy.energy.api.IFItemManager;
import cn.lambdalib2.registry.mc.gui.GuiHandlerBase;
import net.minecraft.world.entity.player.Player;

/**
 * @author WeathFolD
 */
public class ContainerNode extends TechUIContainer<TileNode> {

    public ContainerNode(GuiHandlerBase handler, int id, TileNode _node, Player player) {
        super(handler, id, player, _node);
        initInventory();
    }

    private void initInventory() {
        this.addSlotToContainer(new SlotIFItem(tile, 0, 42, 10));
        this.addSlotToContainer(new SlotIFItem(tile, 1, 42, 80));

        mapPlayerInventory();

        SlotGroup gBatteries = gSlots(0, 1);
        SlotGroup gInv = gRange(2, 2+36);

        addTransferRule(gBatteries, gInv);
        addTransferRule(gInv, IFItemManager.instance::isSupported, gBatteries);
    }

}
