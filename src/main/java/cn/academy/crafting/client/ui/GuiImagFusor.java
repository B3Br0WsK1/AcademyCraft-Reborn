package cn.academy.crafting.client.ui;

import cn.academy.Resources;
import cn.academy.block.container.ContainerImagFusor;
import cn.academy.block.tileentity.TileImagFusor;
import cn.academy.core.client.ui.InventoryPage;
import cn.academy.core.client.ui.TechUI;
import cn.academy.core.client.ui.TechUI.ContainerUI;
import cn.academy.core.client.ui.TechUI.Page;
import cn.academy.core.client.ui.WirelessPage;
import cn.academy.crafting.ImagFusorRecipes.IFRecipe;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.component.ProgressBar;
import cn.lambdalib2.cgui.component.TextBox;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.cgui.loader.CGUIDocument;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class GuiImagFusor {

    private GuiImagFusor() {}

    private static Widget template;

    public static ContainerUI<ContainerImagFusor> apply(ContainerImagFusor container) {
        if (template == null) {
            template = CGUIDocument.read(Resources.getGui("rework/page_imagfusor")).getWidget("main");
        }
        TileImagFusor tile = container.tile;

        Page invPage = InventoryPage.create(template.copy());

        ContainerUI<ContainerImagFusor> ret = new ContainerUI<>(container, invPage, WirelessPage.userPage(tile));

        { // Work progress display
            Widget progWidget = invPage.window().getWidget("progress");
            ProgressBar bar = progWidget.getComponent(ProgressBar.class);
            progWidget.listen(FrameEvent.class, (w, e) -> bar.progress = tile.getWorkProgress());
        }

        { // Imag requirement display
            Widget reqWidget = invPage.window().getWidget("text_imagneeded");
            TextBox text = reqWidget.getComponent(TextBox.class);

            text.content = "IDLE";

            reqWidget.listen(FrameEvent.class, (w, e) -> {
                IFRecipe recipe = tile.getCurrentRecipe();
                text.content = recipe == null ? (tile.isWorking() ? "..." : "IDLE") : String.valueOf(recipe.consumeLiquid);
            });
        }

        ret.infoPage.histogram(
            TechUI.histEnergy(tile::getEnergyForDisplay, tile.getMaxEnergy()),
            TechUI.histPhaseLiquid(tile::getLiquidAmount, tile.getTankSize()));

        return ret;
    }

}
