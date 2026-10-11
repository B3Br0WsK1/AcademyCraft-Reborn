package cn.academy.energy.client.ui;

import cn.academy.block.container.ContainerMatrix;
import cn.academy.block.tileentity.TileMatrix;
import cn.academy.core.client.ui.InventoryPage;
import cn.academy.core.client.ui.TechUI;
import cn.academy.core.client.ui.TechUI.ContainerUI;
import cn.academy.core.client.ui.TechUI.Page;
import cn.academy.energy.MatrixNetProxy;
import cn.academy.energy.MatrixNetProxy.InitData;
import cn.lambdalib2.cgui.component.TextBox;
import cn.lambdalib2.s11n.network.Future;
import cn.lambdalib2.s11n.network.NetworkMessage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import static cn.academy.energy.MatrixNetProxy.*;

@Environment(EnvType.CLIENT)
public final class GuiMatrix2 {

    private GuiMatrix2() {}

    public static ContainerUI<ContainerMatrix> apply(ContainerMatrix container) {
        TileMatrix tile = container.tile;
        Player thePlayer = Minecraft.getInstance().player;
        boolean isPlacer = tile.getPlacerName().equals(thePlayer.getName().getString());

        Page invPage = InventoryPage.create("matrix");

        ContainerUI<ContainerMatrix> ret = new ContainerUI<>(container, invPage);

        send(MSG_GATHER_INFO, tile, Future.create((InitData inf) -> rebuildInfo(ret, tile, thePlayer, isPlacer, inf)));

        return ret;
    }

    private static void rebuildInfo(ContainerUI<ContainerMatrix> ret, TileMatrix tile, Player thePlayer,
                                    boolean isPlacer, InitData data) {
        ret.infoPage.reset();

        ret.infoPage.histogram(
            TechUI.histCapacity(() -> data.load, tile::getCapacity)
        );

        ret.infoPage.seplineInfo()
            .property("owner", tile::getPlacerName)
            .property("range", () -> String.format("%.0f", tile.getRange()))
            .property("bandwidth", () -> tile.getBandwidth() + " IF/T");

        if (data.isInit()) {
            ret.infoPage.sepline("wireless_info");
            if (isPlacer) {
                ret.infoPage
                    .property("ssid", () -> data.ssid, newSSID -> send(MSG_CHANGE_SSID, tile, thePlayer, newSSID))
                    .sepline("change_pass")
                    .property("password", () -> data.pass, newPass -> send(MSG_CHANGE_PASSWORD, tile, thePlayer, newPass),
                        true, true, null);
            } else {
                ret.infoPage
                    .property("ssid", () -> data.ssid)
                    .property("password", () -> data.pass, null, true, true, null);
            }
        } else {
            TextBox[] ssidCell = new TextBox[1];
            TextBox[] passwordCell = new TextBox[1];

            if (isPlacer) {
                ret.infoPage
                    .sepline("wireless_init")
                    .property("ssid", () -> "", s -> {}, false, false, ssidCell)
                    .property("password", () -> "", s -> {}, true, false, passwordCell)
                    .blank(1)
                    .button("INIT", () -> {
                        send(MSG_INIT, tile, ssidCell[0].content, passwordCell[0].content, Future.create((Boolean b) ->
                            send(MSG_GATHER_INFO, tile, Future.create((InitData inf) -> rebuildInfo(ret, tile, thePlayer, isPlacer, inf)))
                        ));
                    });
            } else {
                ret.infoPage.sepline("wireless_noinit");
            }
        }
    }

    private static void send(String channel, Object... args) {
        NetworkMessage.sendToServer(MatrixNetProxy.INSTANCE, channel, args);
    }

}
