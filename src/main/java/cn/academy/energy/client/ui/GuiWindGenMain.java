package cn.academy.energy.client.ui;

import cn.academy.block.container.ContainerWindGenMain;
import cn.academy.block.tileentity.TileWindGenMain;
import cn.academy.core.client.ui.InventoryPage;
import cn.academy.core.client.ui.TechUI.ContainerUI;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class GuiWindGenMain {

    private GuiWindGenMain() {}

    public static ContainerUI<ContainerWindGenMain> apply(ContainerWindGenMain container) {
        TileWindGenMain tile = container.tile;
        ContainerUI<ContainerWindGenMain> ret = new ContainerUI<>(container, InventoryPage.create("windmain"));
        ret.infoPage.property("altitude", () -> tile.getPos().getY());
        return ret;
    }

}
