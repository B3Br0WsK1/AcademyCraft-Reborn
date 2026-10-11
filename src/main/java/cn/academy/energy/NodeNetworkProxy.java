package cn.academy.energy;

import cn.academy.block.tileentity.TileNode;
import cn.academy.energy.api.WirelessHelper;
import cn.academy.energy.impl.NodeConn;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.s11n.network.Future;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.NetworkS11n;
import cn.lambdalib2.util.Side;
import net.minecraft.world.entity.player.Player;

/**
 * Server handlers of the node GUI.
 */
public final class NodeNetworkProxy {

    public static final NodeNetworkProxy INSTANCE = new NodeNetworkProxy();

    public static final String MSG_RENAME = "rename";
    public static final String MSG_CHANGE_PASS = "repass";
    public static final String MSG_INIT = "init";
    public static final String MSG_QUERY_LINK = "query_link";

    private NodeNetworkProxy() {}

    @StateEventCallback
    private static void init(StateEvents.Init ev) {
        NetworkS11n.addDirectInstance(INSTANCE);
    }

    @Listener(channel=MSG_RENAME, side=Side.SERVER)
    private void rename(Player player, TileNode node, String name) {
        if (player.getName().getString().equals(node.getPlacerName())) {
            node.setNodeName(name);
        }
    }

    @Listener(channel=MSG_CHANGE_PASS, side=Side.SERVER)
    private void changePassword(Player player, TileNode node, String name) {
        if (player.getName().getString().equals(node.getPlacerName())) {
            node.setPassword(name);
        }
    }

    @Listener(channel=MSG_QUERY_LINK, side=Side.SERVER)
    private void queryIsLinked(TileNode node, Future<Boolean> future) {
        future.sendResult(WirelessHelper.isNodeLinked(node));
    }

    @Listener(channel=MSG_INIT, side=Side.SERVER)
    private void initLoad(TileNode node, Future<Integer> future) {
        NodeConn conn = WirelessHelper.getNodeConn(node);
        future.sendResult(conn == null ? 0 : conn.getLoad());
    }

}
