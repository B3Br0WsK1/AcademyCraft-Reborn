package cn.lambdalib2.s11n.network;

import cn.lambdalib2.util.Debug;
import cn.lambdalib2.util.SideUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * The single custom payload channel used by LambdaLib2 messages. Each payload carries a "kind" byte (which
 *  sub-system it belongs to) and the raw serialized bytes.
 */
public final class LLNetwork {

    public static final byte KIND_MESSAGE = 0;
    public static final byte KIND_EVENT = 1;
    public static final byte KIND_MULTIBLOCK = 2;

    public record Payload(byte kind, byte[] data) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<Payload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("lambdalib2", "msg"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeByte(p.kind);
                    buf.writeByteArray(p.data);
                },
                buf -> new Payload(buf.readByte(), buf.readByteArray()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public interface Handler {
        /**
         * @param buf    the message data
         * @param sender the sending player when received on server, null when received on client
         */
        void handle(ByteBuf buf, ServerPlayer sender);
    }

    private static final Handler[] handlers = new Handler[8];
    private static Consumer<Payload> clientSender;
    private static boolean initialized = false;

    private LLNetwork() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        PayloadTypeRegistry.serverboundPlay().register(Payload.TYPE, Payload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Payload.TYPE, Payload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Payload.TYPE, (payload, ctx) -> dispatch(payload, ctx.player()));
    }

    public static void _setClientSender(Consumer<Payload> sender) {
        clientSender = sender;
    }

    public static void registerHandler(byte kind, Handler handler) {
        handlers[kind] = handler;
    }

    public static void dispatch(Payload payload, ServerPlayer sender) {
        Handler h = handlers[payload.kind];
        if (h == null) {
            Debug.warn("No handler for LL2 payload kind " + payload.kind);
            return;
        }
        h.handle(Unpooled.wrappedBuffer(payload.data), sender);
    }

    public static byte[] toBytes(ByteBuf buf) {
        byte[] ret = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), ret);
        return ret;
    }

    // Sending

    public static void sendToServer(byte kind, ByteBuf data) {
        Debug.assertNotNull(clientSender, "Client network not initialized").accept(new Payload(kind, toBytes(data)));
    }

    public static void sendTo(Player player, byte kind, ByteBuf data) {
        if (player instanceof ServerPlayer sp) {
            ServerPlayNetworking.send(sp, new Payload(kind, toBytes(data)));
        } else {
            Debug.warn("sendTo called with non-server player " + player);
        }
    }

    public static void sendToAll(byte kind, ByteBuf data) {
        MinecraftServer server = SideUtils.getServer();
        if (server == null) return;
        Payload p = new Payload(kind, toBytes(data));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, p);
        }
    }

    public static void sendToAllAround(TargetPoint point, byte kind, ByteBuf data) {
        MinecraftServer server = SideUtils.getServer();
        if (server == null) return;
        Payload p = new Payload(kind, toBytes(data));
        double rangeSq = point.range * point.range;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level().dimension().equals(point.dimension) &&
                    player.distanceToSqr(point.x, point.y, point.z) <= rangeSq) {
                ServerPlayNetworking.send(player, p);
            }
        }
    }

    public static void sendToDimension(ResourceKey<Level> dimension, byte kind, ByteBuf data) {
        MinecraftServer server = SideUtils.getServer();
        if (server == null) return;
        Payload p = new Payload(kind, toBytes(data));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level().dimension().equals(dimension)) {
                ServerPlayNetworking.send(player, p);
            }
        }
    }
}
