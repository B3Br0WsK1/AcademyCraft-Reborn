package cn.academy.crafting;

import cn.academy.block.tileentity.TileMetalFormer;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.s11n.network.Future;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.NetworkS11n;
import cn.lambdalib2.util.Side;

/**
 * Server handler of the metal former GUI.
 */
public final class MFNetDelegate {

    public static final MFNetDelegate INSTANCE = new MFNetDelegate();

    public static final String MSG_ALTERNATE = "alt";

    private MFNetDelegate() {}

    @StateEventCallback
    private static void init(StateEvents.Init ev) {
        NetworkS11n.addDirectInstance(INSTANCE);
    }

    @Listener(channel=MSG_ALTERNATE, side=Side.SERVER)
    private void alternate(TileMetalFormer tile, int dir, Future<TileMetalFormer.Mode> fut) {
        tile.cycleMode(dir);
        fut.sendResult(tile.mode);
    }

}
