package cn.lambdalib2.s11n.network;

import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.NetworkMessage.NullablePar;
import cn.lambdalib2.s11n.network.NetworkS11n.ContextException;
import cn.lambdalib2.s11n.network.NetworkS11n.NetS11nAdaptor;
import cn.lambdalib2.util.Side;
import cn.lambdalib2.util.SideUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A value that will be sent back from the other side of the network. Create it at one side, pass it to the other side
 *  inside a network message, and call {@link #sendResult(Object)} there.
 */
public class Future<T> {

    int increm;
    Consumer<T> callback; // Valid only on creation side
    Player creator; // Valid only if created in client

    public static void init() {
        NetworkS11n.addDirect(Future.class, new S11nHandler());
        NetworkS11n.register(FutureManager.class);
    }

    public static <T> Future<T> create(Consumer<T> callback) {
        return FutureManager.instance.create(callback);
    }

    public void sendResult(T value) {
        FutureManager.instance.sendResult(this, value);
    }

    Side getSide() {
        return creator == null ? Side.SERVER : Side.CLIENT;
    }

    public static void onDisconnect() {
        FutureManager.instance.disconnect();
    }

    enum FutureManager {
        instance;

        private static final String MSG_RESULT = "result";

        final ThreadLocal<Context> threadContext = ThreadLocal.withInitial(Context::new);

        <T> Future<T> create(Consumer<T> callback) {
            Context ctx = threadContext.get();

            ++ctx.increm;

            Future<T> fut = new Future<>();
            fut.increm = ctx.increm;
            fut.callback = callback;
            fut.creator = SideUtils.getThePlayer(); // null if in server, thePlayer if in client

            ctx.waitingFutures.put(ctx.increm, fut);

            return fut;
        }

        <T> void sendResult(Future<T> fut, T value) {
            if (fut.getSide() == SideUtils.getRuntimeSide()) {
                throw new IllegalStateException("Trying to sendResult in creation side of Future");
            }

            if (SideUtils.isClient()) {
                NetworkMessage.sendToServer(instance, MSG_RESULT, fut.increm, value);
            } else {
                NetworkMessage.sendTo(fut.creator, instance, MSG_RESULT, fut.increm, value);
            }
        }

        @Listener(channel = MSG_RESULT, side = {Side.CLIENT, Side.SERVER})
        @SuppressWarnings("unchecked")
        private void hReceiveResult(int increm, @NullablePar Object value) {
            Context ctx = threadContext.get();

            Future future = ctx.waitingFutures.get(increm);
            if (future != null) {
                future.callback.accept(value);
                ctx.waitingFutures.remove(future.increm);
            }
        }

        void disconnect() {
            threadContext.get().waitingFutures.clear();
        }
    }

    static class S11nHandler implements NetS11nAdaptor<Future> {

        @Override
        public void write(ByteBuf buf, Future obj) {
            buf.writeInt(obj.increm);
            NetworkS11n.serialize(buf, obj.creator, true);
        }

        @Override
        public Future read(ByteBuf buf) throws ContextException {
            int increm = buf.readInt();
            Player player = NetworkS11n.deserialize(buf);

            Future ret = new Future();
            ret.increm = increm;
            ret.creator = player;

            return ret;
        }

    }

    static class Context {
        int increm;
        Map<Integer, Future> waitingFutures = new HashMap<>();
    }
}
