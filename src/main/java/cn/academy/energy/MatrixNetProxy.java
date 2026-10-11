package cn.academy.energy;

import cn.academy.block.tileentity.TileMatrix;
import cn.academy.energy.api.WirelessHelper;
import cn.academy.energy.impl.WirelessNet;
import cn.academy.event.energy.ChangePassEvent;
import cn.academy.event.energy.CreateNetworkEvent;
import cn.lambdalib2.event.EventHub;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.s11n.SerializeNullable;
import cn.lambdalib2.s11n.SerializeStrategy;
import cn.lambdalib2.s11n.SerializeStrategy.ExposeStrategy;
import cn.lambdalib2.s11n.network.Future;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.NetworkS11n;
import cn.lambdalib2.s11n.network.NetworkS11nType;
import cn.lambdalib2.util.Side;
import net.minecraft.world.entity.player.Player;

/**
 * Server handlers of the matrix GUI.
 */
public final class MatrixNetProxy {

    public static final MatrixNetProxy INSTANCE = new MatrixNetProxy();

    public static final String MSG_GATHER_INFO = "gather";
    public static final String MSG_INIT = "init";
    public static final String MSG_CHANGE_PASSWORD = "pass";
    public static final String MSG_CHANGE_SSID = "ssid";

    private MatrixNetProxy() {}

    @NetworkS11nType
    @SerializeStrategy(strategy=ExposeStrategy.ALL)
    public static class InitData {
        @SerializeNullable
        public String ssid = null;
        @SerializeNullable
        public String pass = null;
        public int load = 0;

        public boolean isInit() {
            return ssid != null;
        }
    }

    @StateEventCallback
    private static void init(StateEvents.Init ev) {
        NetworkS11n.addDirectInstance(INSTANCE);
    }

    @Listener(channel=MSG_GATHER_INFO, side=Side.SERVER)
    private void gatherInfo(TileMatrix matrix, Future<InitData> future) {
        WirelessNet net = WirelessHelper.getWirelessNet(matrix);
        InitData result = new InitData();
        if (net != null) {
            result.ssid = net.getSSID();
            result.pass = net.getPassword();
            result.load = net.getLoad();
        }
        future.sendResult(result);
    }

    @Listener(channel=MSG_INIT, side=Side.SERVER)
    private void initNetwork(TileMatrix matrix, String ssid, String pwd, Future<Boolean> fut) {
        EventHub.BUS.post(new CreateNetworkEvent(matrix, ssid, pwd));
        fut.sendResult(true);
    }

    @Listener(channel=MSG_CHANGE_PASSWORD, side=Side.SERVER)
    private void changePassword(TileMatrix matrix, Player player, String pwd) {
        if (matrix.getPlacerName().equals(player.getName().getString())) {
            EventHub.BUS.post(new ChangePassEvent(matrix, pwd));
        }
    }

    @Listener(channel=MSG_CHANGE_SSID, side=Side.SERVER)
    private void changeSSID(TileMatrix matrix, Player player, String newSSID) {
        if (matrix.getPlacerName().equals(player.getName().getString())) {
            WirelessNet net = WirelessHelper.getWirelessNet(matrix);
            if (net != null) {
                net.setSSID(newSSID);
            }
        }
    }

}
