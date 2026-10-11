/**
 * Copyright (c) Lambda Innovation, 2013-2016
 * This file is part of LambdaLib modding library.
 * https://github.com/LambdaInnovation/LambdaLib
 * Licensed under MIT, see project root for more information.
 */
package cn.lambdalib2.s11n.network;

import cn.lambdalib2.s11n.network.NetworkS11n.ContextException;
import cn.lambdalib2.s11n.network.NetworkS11n.NetS11nAdaptor;
import cn.lambdalib2.util.Debug;
import cn.lambdalib2.util.ReflectionUtils;
import cn.lambdalib2.util.Side;
import cn.lambdalib2.util.SideUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * {@link NetworkMessage} is used for fast messaging of objects (that are usually) across network. <br>
 * <p>
 * Use <code>NetworkMessage.sendXXX</code> to send a message to some object.
 * The object is then retrieved using deserialization in receiver side, and all event
 * listeners (methods decorated with {@link Listener} within the object
 * is invoked with given parameters supplied.
 *
 * @author WeAthFolD
 */
public class NetworkMessage {

    private static final byte MAGIC = 0x3c;

    private static final Map<ChannelID, List<INetworkListener>> cachedListeners = new ConcurrentHashMap<>();
    private static final Map<Class, ClassDelegate> classDelegates = new ConcurrentHashMap<>();

    static {
        NetworkS11n.addDirect(ClassDelegate.class, new NetS11nAdaptor<ClassDelegate>() {
            @Override
            public void write(ByteBuf buf, ClassDelegate obj) {
                NetworkS11n.writeString(buf, obj.type.getName());
            }

            @Override
            public ClassDelegate read(ByteBuf buf) throws ContextException {
                return staticCaller(ReflectionUtils.forName(NetworkS11n.readString(buf)));
            }
        });
    }

    public static void init() {
        LLNetwork.registerHandler(LLNetwork.KIND_MESSAGE, NetworkMessage::handle);
    }

    /**
     * Creates a delegate object that invokes static method listeners of the given class. It shall be used as
     * the instance of network message.
     */
    public static ClassDelegate staticCaller(Class<?> type) {
        return classDelegates.computeIfAbsent(type, ClassDelegate::new);
    }

    /**
     * Send the message to the object itself on the fly.
     */
    public static void sendToSelf(Object instance, String channel, Object... params) {
        processMessage(instance, channel, params);
    }

    public static void sendToServer(Object instance, String channel, Object... params) {
        LLNetwork.sendToServer(LLNetwork.KIND_MESSAGE, encode(instance, channel, params));
    }

    public static void sendTo(Player player, Object instance, String channel, Object... params) {
        LLNetwork.sendTo(player, LLNetwork.KIND_MESSAGE, encode(instance, channel, params));
    }

    public static void sendToPlayers(Player[] players, Object instance, String channel, Object... params) {
        ByteBuf buf = encode(instance, channel, params);
        for (Player player : players) {
            LLNetwork.sendTo(player, LLNetwork.KIND_MESSAGE, buf);
        }
    }

    public static void sendToAll(Object instance, String channel, Object... params) {
        LLNetwork.sendToAll(LLNetwork.KIND_MESSAGE, encode(instance, channel, params));
    }

    public static void sendToAllAround(TargetPoint trg, Object instance, String channel, Object... params) {
        LLNetwork.sendToAllAround(trg, LLNetwork.KIND_MESSAGE, encode(instance, channel, params));
    }

    public static void sendToDimension(ResourceKey<Level> dimension, Object instance, String channel, Object... params) {
        LLNetwork.sendToDimension(dimension, LLNetwork.KIND_MESSAGE, encode(instance, channel, params));
    }

    // ---

    private static ByteBuf encode(Object instance, String channel, Object... params) {
        ByteBuf buf = Unpooled.buffer();
        NetworkS11n.writeString(buf, channel);
        NetworkS11n.serialize(buf, instance, false);
        buf.writeByte(params.length);
        for (Object o : params) {
            buf.writeByte(MAGIC);
            NetworkS11n.serialize(buf, o, true);
        }
        return buf;
    }

    private static void handle(ByteBuf buf, ServerPlayer sender) {
        String channel = null;
        Object instance = null;
        Object[] params;
        try {
            channel = NetworkS11n.readString(buf);
            instance = NetworkS11n.deserialize(buf);
            params = new Object[buf.readByte()];
            for (int i = 0; i < params.length; ++i) {
                final int ii = i;
                byte magic = buf.readByte();
                Debug.assert2(magic == MAGIC, () -> ("Error during serializing param " + ii));
                params[i] = NetworkS11n.deserialize(buf);
            }
        } catch (ContextException e) {
            Debug.debug("Ignored network message " + instance + ", " + channel + ", reason: " + e.getMessage());
            return;
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing network message " + channel + "@" + instance, e);
        }
        processMessage(instance, channel, params);
    }

    private static ChannelID id(Class c, String channel, Side side) {
        return new ChannelID(c, channel, side);
    }

    /**
     * Invoked at callee side. Send the message to the instance.
     */
    private static void processMessage(Object instance, String channel, Object... params) {
        if (instance instanceof IMessageDelegate) {
            ((IMessageDelegate) instance).onMessage(channel, params);
        }

        Side side = SideUtils.getRuntimeSide();
        List<INetworkListener> listeners = getListeners(instance, channel, side);
        for (INetworkListener m : listeners) {
            invokeListener(m, channel, instance, params);
        }
    }

    private static void invokeListener(INetworkListener m, String channel, Object instance, Object... params) {
        // Check parameter size
        final int paramc = m.getParameterCount();
        if (paramc > params.length) {
            throw new RuntimeException("Too few arguments in event " + eventSignature(instance, channel)
                    + " for event listener [" + m + "]. Expected at least " + paramc + " arguments");
        } else {
            Object[] paramsArg;
            if (paramc == params.length || paramc == -1) {
                paramsArg = params;
            } else {
                paramsArg = Arrays.copyOf(params, paramc);
            }

            try {
                m.invoke(instance, paramsArg);
            } catch (IllegalArgumentException e) {
                Debug.error("Illegal argument for event listener " + m, e);
            } catch (InvocationTargetException e) {
                Debug.error("Error during network message " + eventSignature(instance, channel), e.getCause());
            } catch (Exception e) {
                Debug.error("Error during network message.", e);
            }
        }
    }

    private static String eventSignature(Object instance, String channel) {
        return (instance == null ? "null" : instance.getClass().getName()) + "#" + channel;
    }

    private static List<INetworkListener> getListeners(Object instance, String channel, Side side) {
        final Class type = instance.getClass();
        final ChannelID cid = id(type, channel, side);

        return cachedListeners.computeIfAbsent(cid, k -> {
            List<INetworkListener> result = new ArrayList<>();
            buildCache(k, result);
            return result;
        });
    }

    private static boolean matches(ChannelID cid, Method m) {
        Listener anno = m.getAnnotation(Listener.class);

        if (anno == null || !anno.channel().equals(cid.channel)) {
            return false;
        } else {
            for (Side s : anno.side()) {
                if (s == cid.side) {
                    return true;
                }
            }
            return false;
        }
    }

    private static void buildCache(ChannelID cid, List<INetworkListener> out) {
        out.addAll(ReflectionUtils.getAccessibleMethods(cid.c)
                .stream()
                .filter(m -> matches(cid, m))
                .map(NetworkMessage::methodListener)
                .collect(Collectors.toList()));
    }

    private static INetworkListener methodListener(Method m) {
        return new INetworkListener() {

            final BitSet nullUnchecked = new BitSet();

            {
                Parameter[] pars = m.getParameters();
                for (int i = 0; i < m.getParameterCount(); ++i) {
                    if (pars[i].isAnnotationPresent(NullablePar.class)) {
                        nullUnchecked.set(i);
                    }
                }
            }

            @Override
            public void invoke(Object instance, Object... args) throws Exception {
                for (int i = 0; i < args.length; ++i) { // Null check
                    if (!nullUnchecked.get(i)) {
                        if (args[i] == null) {
                            return;
                        }
                    }
                }
                m.invoke(instance, args);
            }

            @Override
            public int getParameterCount() {
                return m.getParameterCount();
            }

            @Override
            public String toString() {
                return m.toString();
            }
        };
    }

    /**
     * Annotates on network message listener methods. The method can contain arbitary parameters. The serialized
     * parameters should be able to be passed into the method, or it results in an exception.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Listener {
        /**
         * @return The channel that this method listens
         */
        String channel();

        /**
         * @return The side(s) that this listener receives event
         */
        Side[] side();
    }

    /**
     * Network handlers objects with this interface will have {@link #onMessage(String, Object...)} invoked every time
     * a message is handled.
     */
    public interface IMessageDelegate {

        void onMessage(String channel, Object... args);

    }

    /**
     * Indicate that this parameter can be transformed & deserialized as null.
     */
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface NullablePar {
    }

    public interface INetworkListener {
        void invoke(Object instance, Object... args) throws Exception;

        /**
         * Get the parameter limit of this listener. If return -1 there is no limit.
         */
        default int getParameterCount() {
            return -1;
        }
    }

    private record ChannelID(Class c, String channel, Side side) {
    }

    public static class ClassDelegate implements IMessageDelegate {

        final Class type;
        final Map<ChannelID, List<INetworkListener>> cache = new ConcurrentHashMap<>();

        ClassDelegate(Class<?> _type) {
            type = _type;
        }

        @Override
        public void onMessage(String channel, Object... params) {
            Side side = SideUtils.getRuntimeSide();
            ChannelID id = id(type, channel, side);

            List<INetworkListener> list = cache.computeIfAbsent(id, key -> {
                ArrayList<INetworkListener> ret = new ArrayList<>();
                for (Method m : type.getDeclaredMethods()) {
                    if (Modifier.isStatic(m.getModifiers()) && matches(key, m)) {
                        m.setAccessible(true);
                        ret.add(methodListener(m));
                    }
                }
                return ret;
            });

            for (INetworkListener listener : list) {
                invokeListener(listener, id.channel, null, params);
            }
        }
    }

}
