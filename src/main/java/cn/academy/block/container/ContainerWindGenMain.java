package cn.academy.block.container;

import cn.academy.ACItems;
import cn.academy.block.tileentity.TileWindGenMain;
import cn.lambdalib2.registry.mc.gui.GuiHandlerBase;
import net.minecraft.world.entity.player.Player;

/**
 * @author WeAthFolD
 */
public class ContainerWindGenMain extends TechUIContainer<TileWindGenMain> {

    public ContainerWindGenMain(GuiHandlerBase handler, int id, Player _player, TileWindGenMain _tile) {
        super(handler, id, _player, _tile);

        this.addSlotToContainer(new SlotFan(tile, 0, 78, 9));

        mapPlayerInventory();

        SlotGroup gInv = gRange(1, 1+36), gFan = gSlots(0);

        addTransferRule(gFan, gInv);
        addTransferRule(gInv, stack -> stack.getItem() == ACItems.windgen_fan, gFan);
    }

}
