package cn.academy.block.container;

import cn.lambdalib2.registry.mc.gui.GuiHandlerBase;
import cn.lambdalib2.template.container.CleanContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.BlockEntity;

public class TechUIContainer<T extends BlockEntity> extends CleanContainer {

    public final Player player;
    public final T tile;

    public TechUIContainer(GuiHandlerBase handler, int containerId, Player _player, T _tile) {
        super(handler.getMenuType(), containerId);
        player = _player;
        tile = _tile;
    }

    protected void mapPlayerInventory() {
        int STEP = 18;
        Inventory inv = player.getInventory();

        for(int i = 0; i < 9; ++i) {
            addSlotToContainer(new Slot(inv, i, 6 + i * STEP, 163));
        }

        for(int i = 1; i < 4; ++i) {
            for(int j = 0; j < 9; ++j) {
                int slot = (4 - i) * 9 + j;
                addSlotToContainer(new Slot(inv, slot, 6 + j * STEP, 159 - i * STEP));
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(tile.getBlockPos())) < 64 && !tile.isRemoved();
    }
}
