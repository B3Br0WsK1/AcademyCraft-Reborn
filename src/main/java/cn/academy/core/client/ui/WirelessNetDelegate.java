package cn.academy.core.client.ui;

import cn.academy.energy.api.WirelessHelper;
import cn.academy.energy.api.block.IWirelessMatrix;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.api.block.IWirelessTile;
import cn.academy.energy.api.block.IWirelessUser;
import cn.academy.energy.impl.NodeConn;
import cn.academy.energy.impl.WirelessNet;
import cn.academy.event.energy.LinkNodeEvent;
import cn.academy.event.energy.LinkUserEvent;
import cn.academy.event.energy.UnlinkNodeEvent;
import cn.academy.event.energy.UnlinkUserEvent;
import cn.lambdalib2.event.EventHub;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.s11n.SerializeIncluded;
import cn.lambdalib2.s11n.SerializeNullable;
import cn.lambdalib2.s11n.SerializeStrategy;
import cn.lambdalib2.s11n.SerializeStrategy.ExposeStrategy;
import cn.lambdalib2.s11n.network.Future;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.NetworkS11n;
import cn.lambdalib2.s11n.network.NetworkS11nType;
import cn.lambdalib2.util.Side;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Server side handler of the wireless page network requests. Ported from Scala.
 */
public final class WirelessNetDelegate {

    public static final WirelessNetDelegate INSTANCE = new WirelessNetDelegate();

    public static final String MSG_FIND_NODES = "find_nodes";
    public static final String MSG_USER_CONNECT = "user_connect";
    public static final String MSG_USER_DISCONNECT = "unlink";
    public static final String MSG_NODE_CONNECT = "node_connect";
    public static final String MSG_NODE_DISCONNECT = "node_disconnect";
    public static final String MSG_FIND_NETWORKS = "find_networks";

    private WirelessNetDelegate() {}

    @NetworkS11nType
    public static class UserResult {
        @SerializeIncluded
        @SerializeNullable
        public NodeData linked = null;
        @SerializeIncluded
        public NodeData[] avail = new NodeData[0];
    }

    @NetworkS11nType
    public static class NodeResult {
        @SerializeIncluded
        @SerializeNullable
        public MatrixData linked = null;
        @SerializeIncluded
        public MatrixData[] avail = new MatrixData[0];
    }

    @NetworkS11nType
    @SerializeStrategy(strategy=ExposeStrategy.ALL)
    public static class MatrixData {
        public int x, y, z;
        public String ssid = "";
        public boolean encrypted = false;

        public BlockEntity tile(Level world) {
            BlockEntity te = world.getBlockEntity(new BlockPos(x, y, z));
            return te instanceof IWirelessMatrix ? te : null;
        }
    }

    @NetworkS11nType
    @SerializeStrategy(strategy=ExposeStrategy.ALL)
    public static class NodeData {
        public int x, y, z;
        public boolean encrypted = false;

        public BlockEntity tile(Level world) {
            BlockEntity te = world.getBlockEntity(new BlockPos(x, y, z));
            return te instanceof IWirelessNode ? te : null;
        }
    }

    @StateEventCallback
    private static void init(StateEvents.Init ev) {
        NetworkS11n.addDirectInstance(INSTANCE);
    }

    private static NodeData cvt(NodeConn conn) {
        BlockEntity tile = (BlockEntity) conn.getNode();
        NodeData ret = new NodeData();
        ret.x = tile.getBlockPos().getX();
        ret.y = tile.getBlockPos().getY();
        ret.z = tile.getBlockPos().getZ();
        ret.encrypted = !conn.getNode().getPassword().isEmpty();
        return ret;
    }

    private static MatrixData cvt(WirelessNet net) {
        BlockEntity mat = (BlockEntity) net.getMatrix();
        MatrixData ret = new MatrixData();
        ret.x = mat.getBlockPos().getX();
        ret.y = mat.getBlockPos().getY();
        ret.z = mat.getBlockPos().getZ();
        ret.ssid = net.getSSID();
        ret.encrypted = !net.getPassword().isEmpty();
        return ret;
    }

    @Listener(channel=MSG_FIND_NODES, side=Side.SERVER)
    private void hFindNodes(BlockEntity userTile, Future<UserResult> fut) {
        if (!(userTile instanceof IWirelessUser user)) return;
        NodeConn linked = WirelessHelper.getNodeConn(user);

        List<NodeData> avail = new ArrayList<>();
        for (IWirelessNode node : WirelessHelper.getNodesInRange(userTile.getLevel(), userTile.getBlockPos())) {
            NodeConn conn = WirelessHelper.getNodeConn(node);
            if (conn != null && conn != linked) {
                avail.add(cvt(conn));
            }
        }

        UserResult data = new UserResult();
        data.linked = linked == null ? null : cvt(linked);
        data.avail = avail.toArray(new NodeData[0]);

        fut.sendResult(data);
    }

    @Listener(channel=MSG_FIND_NETWORKS, side=Side.SERVER)
    private void hFindNetworks(BlockEntity nodeTile, Future<NodeResult> fut) {
        if (!(nodeTile instanceof IWirelessNode node)) return;
        WirelessNet linked = WirelessHelper.getWirelessNet(node);

        BlockPos pos = nodeTile.getBlockPos();
        Collection<WirelessNet> networks = WirelessHelper.getNetInRange(nodeTile.getLevel(),
            pos.getX(), pos.getY(), pos.getZ(), node.getRange(), 20);

        List<MatrixData> avail = new ArrayList<>();
        for (WirelessNet net : networks) {
            if (net != linked) {
                avail.add(cvt(net));
            }
        }

        NodeResult data = new NodeResult();
        data.linked = linked == null ? null : cvt(linked);
        data.avail = avail.toArray(new MatrixData[0]);

        fut.sendResult(data);
    }

    @Listener(channel=MSG_USER_CONNECT, side=Side.SERVER)
    private void hUserConnect(BlockEntity user, BlockEntity target, String password, Future<Boolean> fut) {
        if (!(user instanceof IWirelessTile u) || !(target instanceof IWirelessNode node)) return;
        boolean result = !EventHub.BUS.post(new LinkUserEvent(u, node, password));
        fut.sendResult(result);
    }

    @Listener(channel=MSG_USER_DISCONNECT, side=Side.SERVER)
    private void hUserDisconnect(BlockEntity user, Future<Boolean> fut) {
        if (!(user instanceof IWirelessTile u)) return;
        boolean result = !EventHub.BUS.post(new UnlinkUserEvent(u));
        fut.sendResult(result);
    }

    @Listener(channel=MSG_NODE_CONNECT, side=Side.SERVER)
    private void hNodeConnect(BlockEntity node, BlockEntity mat, String pwd, Future<Boolean> fut) {
        if (!(node instanceof IWirelessNode n) || !(mat instanceof IWirelessMatrix m)) return;
        boolean result = !EventHub.BUS.post(new LinkNodeEvent(n, m, pwd));
        fut.sendResult(result);
    }

    @Listener(channel=MSG_NODE_DISCONNECT, side=Side.SERVER)
    private void hNodeDisconnect(BlockEntity node, Future<Boolean> fut) {
        if (!(node instanceof IWirelessNode n)) return;
        EventHub.BUS.post(new UnlinkNodeEvent(n));
        fut.sendResult(true);
    }

}
