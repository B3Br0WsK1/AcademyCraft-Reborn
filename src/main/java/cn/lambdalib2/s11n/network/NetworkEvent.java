package cn.lambdalib2.s11n.network;

import cn.lambdalib2.s11n.network.NetworkS11n.ContextException;
import cn.lambdalib2.util.Side;
import cn.lambdalib2.util.SideUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.*;

/**
 * Sends a serializable object as an "event" to the other side, where handlers registered by
 *  {@link #listen(Class, INetEventHandler)} receive it.
 */
public class NetworkEvent {

    private static final Map<Class<?>, List> handlerClient = new HashMap<>(), handlerServer = new HashMap<>();

    public static void init() {
        LLNetwork.registerHandler(LLNetwork.KIND_EVENT, NetworkEvent::handle);
    }

    public static void sendToServer(Object msg) {
        LLNetwork.sendToServer(LLNetwork.KIND_EVENT, encode(msg));
    }

    public static void sendTo(Object msg, Player player) {
        LLNetwork.sendTo(player, LLNetwork.KIND_EVENT, encode(msg));
    }

    public static void sendToAll(Object msg) {
        LLNetwork.sendToAll(LLNetwork.KIND_EVENT, encode(msg));
    }

    public static void sendToAllAround(Object msg, TargetPoint trg) {
        LLNetwork.sendToAllAround(trg, LLNetwork.KIND_EVENT, encode(msg));
    }

    public static void sendToDimension(Object msg, ResourceKey<Level> dimension) {
        LLNetwork.sendToDimension(dimension, LLNetwork.KIND_EVENT, encode(msg));
    }

    /**
     * Adds a network event handler on both sides.
     */
    public static <T> void listen(Class<T> type, INetEventHandler<T> handler) {
        listen(type, Side.CLIENT, handler);
        listen(type, Side.SERVER, handler);
    }

    /**
     * Adds a network event handler on given side.
     */
    public static <T> void listen(Class<T> type, Side side, INetEventHandler<T> handler) {
        _handlerList(type, side, true).add(handler);
    }

    private static ByteBuf encode(Object msg) {
        ByteBuf buf = Unpooled.buffer();
        NetworkS11n.serialize(buf, msg, false);
        return buf;
    }

    @SuppressWarnings("unchecked")
    private static void handle(ByteBuf buf, ServerPlayer sender) {
        Object object;
        try {
            object = NetworkS11n.deserialize(buf);
        } catch (ContextException exc) {
            return;
        }
        if (object != null) {
            MessageContext ctx = new MessageContext(sender == null ? Side.CLIENT : Side.SERVER, sender);
            _handlerList(object.getClass(), ctx.side, false)
                    .forEach(handler -> ((INetEventHandler) handler).onEvent(object, ctx));
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> List<INetEventHandler<T>> _handlerList(Class<T> type, Side side, boolean create) {
        boolean client = side == Side.CLIENT;
        Map<Class<?>, List> map = client ? handlerClient : handlerServer;
        List<INetEventHandler<T>> ret = map.get(type);

        if (create && ret == null) {
            ret = new ArrayList<>();
            map.put(type, ret);
        }

        return ret == null ? Collections.emptyList() : ret;
    }

    public static class MessageContext {
        public final Side side;
        private final ServerPlayer sender;

        MessageContext(Side side, ServerPlayer sender) {
            this.side = side;
            this.sender = sender;
        }

        /**
         * @return The player that sent the message, only valid in server.
         */
        public ServerPlayer getServerPlayer() {
            return sender;
        }
    }

    public interface INetEventHandler<T> {
        void onEvent(T event, MessageContext ctx);

        default Player getClientPlayer() {
            return SideUtils.getThePlayer();
        }
    }

}
