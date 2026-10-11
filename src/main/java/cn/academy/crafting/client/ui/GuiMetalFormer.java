package cn.academy.crafting.client.ui;

import cn.academy.Resources;
import cn.academy.block.container.ContainerMetalFormer;
import cn.academy.block.tileentity.TileMetalFormer;
import cn.academy.core.client.ui.InventoryPage;
import cn.academy.core.client.ui.TechUI;
import cn.academy.core.client.ui.TechUI.ContainerUI;
import cn.academy.core.client.ui.WirelessPage;
import cn.academy.crafting.MFNetDelegate;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.component.DrawTexture;
import cn.lambdalib2.cgui.component.ProgressBar;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.cgui.event.LeftClickEvent;
import cn.lambdalib2.cgui.loader.CGUIDocument;
import cn.lambdalib2.render.font.IFont.FontAlign;
import cn.lambdalib2.render.font.IFont.FontOption;
import cn.lambdalib2.s11n.network.Future;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.util.Colors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class GuiMetalFormer {

    private GuiMetalFormer() {}

    private static Widget template;

    public static ContainerUI<ContainerMetalFormer> apply(ContainerMetalFormer container) {
        if (template == null) {
            template = CGUIDocument.read(Resources.getGui("rework/page_metalformer")).getWidget("main");
        }
        TileMetalFormer tile = container.tile;

        Widget invWidget = template.copy();

        {
            DrawTexture modeTex = invWidget.getWidget("icon_mode").getComponent(DrawTexture.class);
            modeTex.texture = tile.mode.texture;

            invWidget.getWidget("progress").listen(FrameEvent.class, (w, evt) ->
                w.getComponent(ProgressBar.class).progress = tile.getWorkProgress());

            invWidget.getWidget("btn_left").listen(LeftClickEvent.class, (w, e) ->
                send(MFNetDelegate.MSG_ALTERNATE, tile, -1, Future.create((TileMetalFormer.Mode m) -> modeTex.texture = m.texture)));
            invWidget.getWidget("btn_right").listen(LeftClickEvent.class, (w, e) ->
                send(MFNetDelegate.MSG_ALTERNATE, tile, 1, Future.create((TileMetalFormer.Mode m) -> modeTex.texture = m.texture)));

            FontOption option = new FontOption(10, FontAlign.CENTER, Colors.fromHexColor(0xaaffffff));
            invWidget.getWidget("icon_mode").listen(FrameEvent.class, (w, evt) -> {
                modeTex.texture = tile.mode.texture;
                if (evt.hovering) {
                    TechUI.drawTextBox(tile.mode.toString(), option, 6, -10);
                }
            });
        }

        ContainerUI<ContainerMetalFormer> ret = new ContainerUI<>(container, InventoryPage.create(invWidget),
            WirelessPage.userPage(tile));

        ret.infoPage.histogram(TechUI.histEnergy(tile::getEnergy, tile.getMaxEnergy()));

        return ret;
    }

    private static void send(String channel, Object... args) {
        NetworkMessage.sendToServer(MFNetDelegate.INSTANCE, channel, args);
    }

}
