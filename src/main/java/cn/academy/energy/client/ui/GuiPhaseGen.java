package cn.academy.energy.client.ui;

import cn.academy.block.container.ContainerPhaseGen;
import cn.academy.block.tileentity.TilePhaseGen;
import cn.academy.core.client.ui.InventoryPage;
import cn.academy.core.client.ui.TechUI;
import cn.academy.core.client.ui.TechUI.ContainerUI;
import cn.academy.core.client.ui.WirelessPage;
import cn.lambdalib2.util.Colors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class GuiPhaseGen {

    private GuiPhaseGen() {}

    public static ContainerUI<ContainerPhaseGen> apply(ContainerPhaseGen container) {
        TilePhaseGen tile = container.tile;
        ContainerUI<ContainerPhaseGen> ret = new ContainerUI<>(container, InventoryPage.create("phasegen"),
            WirelessPage.userPage(tile));
        ret.infoPage.histogram(
            TechUI.histEnergy(tile::getEnergy, tile.bufferSize),
            new TechUI.HistElement("IF", Colors.fromHexColor(0xffb983fb),
                () -> (double) tile.getLiquidAmount() / tile.getTankSize(),
                () -> String.format("%d mB", tile.getLiquidAmount())));
        return ret;
    }

}
