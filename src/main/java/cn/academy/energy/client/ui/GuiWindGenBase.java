package cn.academy.energy.client.ui;

import cn.academy.Resources;
import cn.academy.block.container.ContainerWindGenBase;
import cn.academy.block.tileentity.TileWindGenBase;
import cn.academy.core.client.ui.InventoryPage;
import cn.academy.core.client.ui.TechUI;
import cn.academy.core.client.ui.TechUI.ContainerUI;
import cn.academy.core.client.ui.WirelessPage;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.component.DrawTexture;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.cgui.loader.CGUIDocument;
import cn.lambdalib2.util.Colors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class GuiWindGenBase {

    private GuiWindGenBase() {}

    private static Widget template;

    public static ContainerUI<ContainerWindGenBase> apply(ContainerWindGenBase container) {
        if (template == null) {
            template = CGUIDocument.read(Resources.getGui("rework/page_windbase")).getWidget("main");
        }
        TileWindGenBase tile = container.tile;
        Widget main = template.copy();

        {
            Widget ui = main.getWidget("ui_block");
            DrawTexture texMain = ui.getWidget("icon_main").getComponent(DrawTexture.class);
            DrawTexture texMiddle = ui.getWidget("icon_middle").getComponent(DrawTexture.class);
            DrawTexture texBase = ui.getWidget("icon_base").getComponent(DrawTexture.class);

            float a0 = 0.2f, a1 = 0.6f, a2 = 1.0f;

            ui.listen(FrameEvent.class, (w, e) -> {
                float amain, amiddle, abase;
                switch (tile.getCompleteness()) {
                    case COMPLETE_NOT_WORKING -> { amain = a1; amiddle = a2; abase = a2; }
                    case COMPLETE -> { amain = a2; amiddle = a2; abase = a2; }
                    case NO_TOP -> { amain = a0; amiddle = a2; abase = a2; }
                    default -> { amain = a0; amiddle = a0; abase = a2; }
                }
                texBase.color.setAlpha(Colors.f2i(abase));
                texMain.color.setAlpha(Colors.f2i(amain));
                texMiddle.color.setAlpha(Colors.f2i(amiddle));
            });
        }

        ContainerUI<ContainerWindGenBase> ret = new ContainerUI<>(container, InventoryPage.create(main),
            WirelessPage.userPage(tile));

        ret.infoPage
            .histogram(TechUI.histBuffer(tile::getEnergy, tile.bufferSize))
            .seplineInfo()
            .property("altitude", () -> tile.getPos().getY());

        return ret;
    }

}
