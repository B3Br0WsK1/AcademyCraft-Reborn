/**
 * Copyright (c) Lambda Innovation, 2013-2016
 * This file is part of LambdaLib modding library.
 * https://github.com/LambdaInnovation/LambdaLib
 * Licensed under MIT, see project root for more information.
 */
package cn.lambdalib2.s11n.network;

import cn.lambdalib2.s11n.SerializationHelper;
import cn.lambdalib2.s11n.SerializeDynamic;
import cn.lambdalib2.s11n.SerializeNullable;
import cn.lambdalib2.util.Debug;
import cn.lambdalib2.util.ReflectionUtils;
import cn.lambdalib2.util.SideUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Supplier;

/**
 * This class handles recursive s11n on netty {@link ByteBuf}.
 *
 * @author WeAthFolD
 */
public class NetworkS11n {

    // ---
    private static final SerializationHelper serHelper = new SerializationHelper();
    private static final int IDX_NULL = -1, IDX_ARRAY = -2;
    private static final BiMap<Integer, Class<?>> serTypesHashLookup = HashBiMap.create();

    private static final Map<Class<?>, NetS11nAdaptor> adaptors = new HashMap<>();
    private static final Map<Class, Supplier> suppliers = new HashMap<>();
    private static final Map<Class, List<Field>> fieldCache = new HashMap<>();

    private static final byte MAGIC = 0x47;

    // ---- Basic buffer helpers

    public static void writeString(ByteBuf buf, String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        writeVarInt(buf, bytes.length);
        buf.writeBytes(bytes);
    }

    public static String readString(ByteBuf buf) {
        int len = readVarInt(buf);
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static void writeVarInt(ByteBuf buf, int value) {
        while ((value & -128) != 0) {
            buf.writeByte(value & 127 | 128);
            value >>>= 7;
        }
        buf.writeByte(value);
    }

    public static int readVarInt(ByteBuf buf) {
        int value = 0;
        int shift = 0;
        byte b;
        do {
            b = buf.readByte();
            value |= (b & 127) << shift;
            shift += 7;
            if (shift > 35) throw new RuntimeException("VarInt too big");
        } while ((b & 128) != 0);
        return value;
    }

    public static void writeTag(ByteBuf buf, CompoundTag tag) {
        ByteBuf tmp = Unpooled.buffer();
        try {
            NbtIo.write(tag, new io.netty.buffer.ByteBufOutputStream(tmp));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        writeVarInt(buf, tmp.readableBytes());
        buf.writeBytes(tmp);
    }

    public static CompoundTag readTag(ByteBuf buf) {
        int len = readVarInt(buf);
        ByteBuf slice = buf.readSlice(len);
        try {
            return NbtIo.read(new io.netty.buffer.ByteBufInputStream(slice), NbtAccounter.unlimitedHeap());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static RegistryAccess registryAccess() {
        if (SideUtils.isClient()) {
            Level lvl = SideUtils.getThePlayer() != null ? SideUtils.getThePlayer().level() : null;
            if (lvl == null) throw new ContextException("No client level");
            return lvl.registryAccess();
        } else {
            return Debug.assertNotNull(SideUtils.getServer(), "No server").registryAccess();
        }
    }

    // default s11n types
    static {
        { // Byte
            NetS11nAdaptor<Byte> adp = new NetS11nAdaptor<Byte>() {
                public void write(ByteBuf buf, Byte value) {
                    buf.writeByte(value);
                }

                public Byte read(ByteBuf buf) {
                    return buf.readByte();
                }
            };
            addDirect(byte.class, adp);
            addDirect(Byte.class, adp);
        }
        { // Short
            NetS11nAdaptor<Short> adp = new NetS11nAdaptor<Short>() {
                public void write(ByteBuf buf, Short value) {
                    buf.writeShort(value);
                }

                public Short read(ByteBuf buf) {
                    return buf.readShort();
                }
            };
            addDirect(short.class, adp);
            addDirect(Short.class, adp);
        }
        { // Int
            NetS11nAdaptor<Integer> adp = new NetS11nAdaptor<Integer>() {
                public void write(ByteBuf buf, Integer value) {
                    buf.writeInt(value);
                }

                public Integer read(ByteBuf buf) {
                    return buf.readInt();
                }
            };
            addDirect(int.class, adp);
            addDirect(Integer.class, adp);
        }
        { // Long
            NetS11nAdaptor<Long> adp = new NetS11nAdaptor<Long>() {
                public void write(ByteBuf buf, Long value) {
                    buf.writeLong(value);
                }

                public Long read(ByteBuf buf) {
                    return buf.readLong();
                }
            };
            addDirect(long.class, adp);
            addDirect(Long.class, adp);
        }
        { // Float
            NetS11nAdaptor<Float> adp = new NetS11nAdaptor<Float>() {
                @Override
                public void write(ByteBuf buf, Float obj) {
                    buf.writeFloat(obj);
                }

                @Override
                public Float read(ByteBuf buf) {
                    return buf.readFloat();
                }
            };
            addDirect(float.class, adp);
            addDirect(Float.class, adp);
        }
        { // Double
            NetS11nAdaptor<Double> adp = new NetS11nAdaptor<Double>() {
                @Override
                public void write(ByteBuf buf, Double obj) {
                    buf.writeDouble(obj);
                }

                @Override
                public Double read(ByteBuf buf) {
                    return buf.readDouble();
                }
            };
            addDirect(double.class, adp);
            addDirect(Double.class, adp);
        }
        { // Boolean
            NetS11nAdaptor<Boolean> adp = new NetS11nAdaptor<Boolean>() {
                @Override
                public void write(ByteBuf buf, Boolean obj) {
                    buf.writeBoolean(obj);
                }

                @Override
                public Boolean read(ByteBuf buf) throws ContextException {
                    return buf.readBoolean();
                }
            };
            addDirect(boolean.class, adp);
            addDirect(Boolean.class, adp);
        }
        { // Char
            NetS11nAdaptor<Character> adp = new NetS11nAdaptor<Character>() {
                @Override
                public void write(ByteBuf buf, Character obj) {
                    buf.writeChar(obj);
                }

                @Override
                public Character read(ByteBuf buf) throws ContextException {
                    return buf.readChar();
                }
            };
            addDirect(char.class, adp);
            addDirect(Character.class, adp);
        }

        addDirect(String.class, new NetS11nAdaptor<String>() {
            @Override
            public void write(ByteBuf buf, String obj) {
                writeString(buf, obj);
            }

            @Override
            public String read(ByteBuf buf) {
                return readString(buf);
            }
        });

        addDirect(ByteBuf.class, new NetS11nAdaptor<ByteBuf>() {
            @Override
            public void write(ByteBuf buf, ByteBuf obj) {
                ByteBuf dup = obj.duplicate();
                writeVarInt(buf, dup.readableBytes());
                buf.writeBytes(dup);
            }

            @Override
            public ByteBuf read(ByteBuf buf) throws ContextException {
                int len = readVarInt(buf);
                ByteBuf buf2 = Unpooled.buffer(len);
                buf.readBytes(buf2, len);
                return buf2;
            }
        });

        // Meta: Class
        addDirect(Class.class, new NetS11nAdaptor<Class>() {
            @Override
            public void write(ByteBuf buf, Class obj) {
                Integer idx = serTypesHashLookup.inverse().get(obj);
                if (idx == null) throw new IllegalArgumentException(obj + " is not a network s11n type");

                buf.writeInt(idx);
            }

            @Override
            public Class read(ByteBuf buf) throws ContextException {
                return serTypesHashLookup.get(buf.readInt());
            }
        });

        // Basic collection types
        addDirect(List.class, new NetS11nAdaptor<List>() {
            @Override
            public void write(ByteBuf buf, List obj) {
                writeVarInt(buf, obj.size());
                for (Object elem : obj) {
                    serialize(buf, elem, true);
                }
            }

            @Override
            public List read(ByteBuf buf) {
                ArrayList<Object> ret = new ArrayList<>();
                int size = readVarInt(buf);
                for (int i = 0; i < size; ++i) {
                    ret.add(deserialize(buf));
                }
                return ret;
            }
        });
        addDirect(Map.class, new NetS11nAdaptor<Map>() {
            @Override
            public void write(ByteBuf buf, Map obj) {
                writeVarInt(buf, obj.size());
                final Set<Entry> set = obj.entrySet();
                for (Entry e : set) {
                    serialize(buf, e.getKey(), false);
                    serialize(buf, e.getValue(), true);
                }
            }

            @Override
            public Map read(ByteBuf buf) {
                HashMap<Object, Object> ret = new HashMap<>();
                int size = readVarInt(buf);
                for (int i = 0; i < size; ++i) {
                    ret.put(deserialize(buf), deserialize(buf));
                }
                return ret;
            }
        });
        addDirect(Set.class, new NetS11nAdaptor<Set>() {
            @Override
            public void write(ByteBuf buf, Set obj) {
                writeVarInt(buf, obj.size());

                for (Object o : obj) {
                    serialize(buf, o, false);
                }
            }

            @Override
            public Set read(ByteBuf buf) throws ContextException {
                int size = readVarInt(buf);
                Set<Object> ret = new HashSet<>();

                while (size-- > 0) {
                    ret.add(deserialize(buf));
                }

                return ret;
            }
        });
        addDirect(BitSet.class, new NetS11nAdaptor<BitSet>() {
            @Override
            public void write(ByteBuf buf, BitSet obj) {
                byte[] bytes = obj.toByteArray();
                writeVarInt(buf, bytes.length);
                buf.writeBytes(bytes);
            }

            @Override
            public BitSet read(ByteBuf buf) {
                int readBytes = readVarInt(buf);
                byte[] bytes = ByteBufUtil.getBytes(buf.readBytes(readBytes));
                return BitSet.valueOf(bytes);
            }
        });
        addDirect(UUID.class, new NetS11nAdaptor<UUID>() {
            @Override
            public void write(ByteBuf buf, UUID obj) {
                buf.writeLong(obj.getMostSignificantBits());
                buf.writeLong(obj.getLeastSignificantBits());
            }

            @Override
            public UUID read(ByteBuf buf) {
                return new UUID(buf.readLong(), buf.readLong());
            }
        });
        //

        // MC types
        addDirect(CompoundTag.class, new NetS11nAdaptor<CompoundTag>() {
            @Override
            public void write(ByteBuf buf, CompoundTag obj) {
                writeTag(buf, obj);
            }

            @Override
            public CompoundTag read(ByteBuf buf) {
                return readTag(buf);
            }
        });
        addDirect(Identifier.class, new NetS11nAdaptor<Identifier>() {
            @Override
            public void write(ByteBuf buf, Identifier obj) {
                writeString(buf, obj.toString());
            }

            @Override
            public Identifier read(ByteBuf buf) {
                return Identifier.parse(readString(buf));
            }
        });
        addDirect(BlockPos.class, new NetS11nAdaptor<BlockPos>() {
            @Override
            public void write(ByteBuf buf, BlockPos obj) {
                buf.writeLong(obj.asLong());
            }

            @Override
            public BlockPos read(ByteBuf buf) {
                return BlockPos.of(buf.readLong());
            }
        });
        addDirect(Entity.class, new NetS11nAdaptor<Entity>() {
            @Override
            public void write(ByteBuf buf, Entity obj) {
                writeString(buf, obj.level().dimension().identifier().toString());
                if (obj instanceof EnderDragonPart part) {
                    buf.writeInt(part.parentMob.getId());
                    buf.writeInt(obj.getId());
                } else if (obj instanceof EnderDragon) {
                    buf.writeInt(obj.getId());
                    buf.writeInt(0);
                } else {
                    buf.writeInt(obj.getId());
                }
            }

            @Override
            public Entity read(ByteBuf buf) {
                if (SideUtils.isClient() && SideUtils.getThePlayer() == null) {
                    throw new ContextException("Client player hasn't been setup yet");
                }

                Level wrld = SideUtils.getWorld(dimKey(readString(buf)));
                if (wrld == null) {
                    throw new ContextException("Invalid world");
                } else {
                    int entityId = buf.readInt();
                    Entity ret = wrld.getEntity(entityId);
                    if (ret == null) {
                        throw new ContextException("No entity with such ID");
                    } else {
                        if (ret instanceof EnderDragon dragon) {
                            int partEntityId = buf.readInt();
                            if (partEntityId != 0) {
                                for (EnderDragonPart part : dragon.getSubEntities()) {
                                    if (part.getId() == partEntityId) {
                                        return part;
                                    }
                                }
                            }
                        }
                        return ret;
                    }
                }
            }
        });
        addDirect(Level.class, new NetS11nAdaptor<Level>() {
            @Override
            public void write(ByteBuf buf, Level obj) {
                writeString(buf, obj.dimension().identifier().toString());
            }

            @Override
            public Level read(ByteBuf buf) throws ContextException {
                Level wrld = SideUtils.getWorld(dimKey(readString(buf)));
                if (wrld == null) {
                    throw new ContextException("invalid world");
                } else {
                    return wrld;
                }
            }
        });
        addDirect(BlockEntity.class, new NetS11nAdaptor<BlockEntity>() {
            @Override
            public void write(ByteBuf buf, BlockEntity obj) {
                serializeWithHint(buf, obj.getLevel(), Level.class);
                buf.writeLong(obj.getBlockPos().asLong());
            }

            @Override
            public BlockEntity read(ByteBuf buf) throws ContextException {
                Level world = deserializeWithHint(buf, Level.class);
                BlockPos pos = BlockPos.of(buf.readLong());
                BlockEntity tileEntity = world.getBlockEntity(pos);

                if (tileEntity == null) {
                    throw new ContextException("No such TileEntity is present");
                } else {
                    return tileEntity;
                }
            }
        });
        addDirect(Vec3.class, new NetS11nAdaptor<Vec3>() {
            @Override
            public void write(ByteBuf buf, Vec3 obj) {
                buf.writeDouble(obj.x);
                buf.writeDouble(obj.y);
                buf.writeDouble(obj.z);
            }

            @Override
            public Vec3 read(ByteBuf buf) throws ContextException {
                return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            }
        });
        addDirect(ItemStack.class, new NetS11nAdaptor<ItemStack>() {
            @Override
            public void write(ByteBuf buf, ItemStack obj) {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(new RegistryFriendlyByteBuf(buf, registryAccess()), obj);
            }

            @Override
            public ItemStack read(ByteBuf buf) throws ContextException {
                return ItemStack.OPTIONAL_STREAM_CODEC.decode(new RegistryFriendlyByteBuf(buf, registryAccess()));
            }
        });
    }

    public static ResourceKey<Level> dimKey(String id) {
        return ResourceKey.create(Registries.DIMENSION, Identifier.parse(id));
    }

    private NetworkS11n() {
    }

    public static <T> void addDirect(Class<T> type, NetS11nAdaptor<? super T> adaptor) {
        register(type);
        adaptors.put(type, adaptor);
        serHelper.regS11nType(type);
    }

    @SuppressWarnings("unchecked")
    public static <T> void addDirectInstance(T instance) {
        register(instance.getClass());
        addDirect((Class<T>) instance.getClass(), new NetS11nAdaptor<T>() {
            @Override
            public void write(ByteBuf buf, T obj) {
            }

            @Override
            public T read(ByteBuf buf) throws ContextException {
                return instance;
            }
        });
    }

    public static <T> void addSupplier(Class<T> type, Supplier<? extends T> supplier) {
        suppliers.put(type, supplier);
    }

    /**
     * Registers all {@link NetworkS11nType}s and {@link RegNetS11nAdapter}s found by the annotation scanner.
     */
    public static void registerAll() {
        ReflectionUtils.getClasses(NetworkS11nType.class).forEach(NetworkS11n::register);
        ReflectionUtils.getFields(RegNetS11nAdapter.class).forEach(field -> {
            try {
                RegNetS11nAdapter anno = field.getAnnotation(RegNetS11nAdapter.class);
                NetS11nAdaptor adaptor = ((NetS11nAdaptor) field.get(null));
                Debug.assertNotNull(adaptor);
                addDirect(anno.value(), adaptor);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    /**
     * Registers an object for network s11n. You MUST do this on any object that will be serialized before
     * any serialization is actually performed. The id table is used in both C and S to reduce data redundancy, so
     * client programmers must keep registration in both sides consistent.
     */
    public static synchronized void register(Class<?> type) {
        if (!serTypesHashLookup.containsValue(type)) {
            int hash = type.getName().hashCode();
            Class<?> existing = serTypesHashLookup.get(hash);
            if (existing != null && existing != type) {
                throw new RuntimeException("Hash collision in net s11n: " + existing + " and " + type);
            }
            serTypesHashLookup.put(hash, type);
            serHelper.regS11nType(type);
        }
    }

    private static void writeTypeIndex(ByteBuf buf, Class type) {
        buf.writeByte(MAGIC);
        if (type.isArray()) {
            buf.writeInt(IDX_ARRAY);
            writeTypeIndex(buf, type.getComponentType());
        } else {
            int idx = typeIndex(type);
            if (idx == -1) {
                throw new RuntimeException("Type " + type + " not registered for net serialization");
            }
            buf.writeInt(idx);
        }
    }

    private static Class readTypeIndex(ByteBuf buf) {
        byte magic = buf.readByte();
        Debug.assert2(magic == MAGIC, buf::toString);
        int idx = buf.readInt();
        if (idx == IDX_NULL) {
            return null;
        } else if (idx == IDX_ARRAY) {
            return getArrayClass(readTypeIndex(buf));
        } else {
            Class ret = serTypesHashLookup.get(idx);
            Debug.assertNotNull(ret, () -> "No class registered for hash " + idx);
            return ret;
        }
    }

    private static <T> Class getArrayClass(Class<T> component) {
        return Array.newInstance(component, 0).getClass();
    }

    /**
     * Serializes a object.
     *
     * @param buf      The buffer to serialize the object into
     * @param obj      The object to be serialized, whose type MUST be previously registered using {@link #register(Class)}
     * @param nullable if paramterer is allowed to be <code>null</code>
     * @throws RuntimeException if serialization failed
     */
    @SuppressWarnings("unchecked")
    public static void serialize(ByteBuf buf, Object obj, boolean nullable) {
        if (obj == null) {
            if (nullable) {
                buf.writeByte(MAGIC);
                buf.writeInt(IDX_NULL);
            } else {
                throw new NullPointerException("Trying to serialize a null object where it's not accepted");
            }
        } else {
            Class type = obj.getClass();
            writeTypeIndex(buf, type);
            serializeWithHint(buf, obj, type);
        }
    }

    /**
     * serialize an object without writing the object type index into the buffer. Used to reduce data redundancy.
     * Use {@link #deserializeWithHint} with the same signature to recover the object.
     */
    @SuppressWarnings("unchecked")
    public static <T> void serializeWithHint(ByteBuf buf, T obj, Class<? super T> type) {
        _check(obj != null, "Hintted serialization doesn't take null");

        NetS11nAdaptor<? super T> adaptor = (NetS11nAdaptor) _adaptor(type);
        if (adaptor != null) { // Serialize direct types
            adaptor.write(buf, obj);
        } else if (type.isEnum()) { // Serialize enum
            buf.writeByte(((Enum) obj).ordinal());
        } else if (type.isArray()) { // Serialize array
            int length = Array.getLength(obj);
            Preconditions.checkArgument(length < Short.MAX_VALUE, "Array too large");

            buf.writeShort(length);
            for (int i = 0; i < length; ++i) {
                serialize(buf, Array.get(obj, i), true);
            }
        } else { // Serialize recursive types
            serializeRecursively(buf, obj, type);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> void serializeRecursively(ByteBuf buf, T obj, Class<? super T> type) {
        final List<Field> fields = _sortedFields(type);
        try {
            for (Field f : fields) {
                Object sub = f.get(obj);
                if (_needsTypeIndex(f)) {
                    boolean subNullable = f.isAnnotationPresent(SerializeNullable.class);
                    serialize(buf, sub, subNullable);
                } else {
                    serializeWithHint(buf, sub, (Class) f.getType());
                }
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException("Error serializing object " + obj, e);
        }
    }

    /**
     * Deserializes a object from given buf.
     *
     * @return The deserialized object. Could be null.
     * @throws ContextException if deserialization is interrupted.
     * @throws RuntimeException if deserialization failed non-trivially.
     */
    @SuppressWarnings("unchecked")
    public static <T> T deserialize(ByteBuf buf) {
        Class type = readTypeIndex(buf);

        if (type == null) {
            return null;
        } else {
            Class<T> type2 = (Class<T>) type;
            return deserializeWithHint(buf, type2);
        }
    }

    /**
     * Deserialize an object with given type hint.
     */
    @SuppressWarnings("unchecked")
    public static <T, U extends T> T deserializeWithHint(ByteBuf buf, Class<U> type) {
        NetS11nAdaptor<? super U> adaptor = (NetS11nAdaptor) _adaptor(type);
        if (adaptor != null) {
            return (T) adaptor.read(buf);
        } else if (type.isArray()) { // Deserialize array
            int size = buf.readShort();
            Class componentType = type.getComponentType();

            Object ret = Array.newInstance(componentType, size);
            for (int i = 0; i < size; ++i) {
                Array.set(ret, i, deserialize(buf));
            }

            return (T) ret;
        } else if (type.isEnum()) { // Deserialize enum
            return type.getEnumConstants()[buf.readByte()];
        } else { // Recursive deserialization
            return deserializeRecursively(buf, type);
        }
    }

    public static <T, U extends T> T deserializeRecursively(ByteBuf buf, Class<U> type) {
        try {
            T instance = instantiate(type);
            deserializeRecursivelyInto(buf, instance, type);
            return instance;
        } catch (ContextException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new RuntimeException("Error deserializing type " + type, e);
        }
    }

    public static <T, U extends T> void deserializeRecursivelyInto(ByteBuf buf, T instance, Class<U> type) {
        for (Field f : _sortedFields(type)) {
            // Both nullable and porlymorphic s11n needs type index.
            Object sub;
            if (_needsTypeIndex(f)) {
                sub = deserialize(buf);
            } else {
                sub = deserializeWithHint(buf, f.getType());
            }

            try {
                f.set(instance, sub);
            } catch (IllegalArgumentException | IllegalAccessException exc) {
                throw new RuntimeException("Type mismatch in net s11n: expecting " +
                        f.getType() + ", found " + (sub != null ? sub.getClass() : "NULL"));
            }
        }
    }

    private static int typeIndex(Class<?> type) {
        Class<?> cur = type;
        while (cur != null) {
            Integer hash = serTypesHashLookup.inverse().get(cur);
            if (hash != null)
                return hash;
            cur = cur.getSuperclass();
        }
        // Interfaces (e.g. List, Map, Set implementations)
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for (Class<?> itf : c.getInterfaces()) {
                Integer hash = serTypesHashLookup.inverse().get(itf);
                if (hash != null)
                    return hash;
            }
        }
        return -1;
    }

    private static synchronized List<Field> _sortedFields(Class<?> type) {
        List<Field> ret = fieldCache.get(type);
        if (ret == null) {
            ret = Lists.newArrayList(serHelper.getExposedFields(type));
            // Sort to preserve s11n order
            ret.sort(Comparator.comparing(Field::getName));
            fieldCache.put(type, ret);
        }

        return ret;
    }

    private static boolean _needsTypeIndex(Field f) {
        return f.isAnnotationPresent(SerializeDynamic.class) ||
                f.isAnnotationPresent(SerializeNullable.class);
    }

    @SuppressWarnings("unchecked")
    private static <T> NetS11nAdaptor<? super T> _adaptor(Class<T> topClass) {
        Class<? super T> cur = topClass;
        NetS11nAdaptor<? super T> ret;
        while (cur != null) {
            ret = adaptors.get(cur);
            if (ret != null) {
                return ret;
            }
            cur = cur.getSuperclass();
        }

        for (Class<?> c = topClass; c != null; c = c.getSuperclass()) {
            for (Class<?> itf : c.getInterfaces()) {
                ret = adaptors.get(itf);
                if (ret != null) {
                    return ret;
                }
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> T instantiate(Class<T> type) {
        if (suppliers.containsKey(type)) {
            return (T) suppliers.get(type).get();
        }
        try {
            Constructor<T> ctor = type.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void _check(boolean pred, String errmsg) {
        if (!pred) {
            throw new RuntimeException(errmsg);
        }
    }

    public interface NetS11nAdaptor<T> {

        /**
         * Write the object to given ByteBuf.
         *
         * @param buf The buf to write object to
         * @param obj The object, not null
         */
        void write(ByteBuf buf, T obj);

        /**
         * Read the object from given ByteBuf, MUST NOT BE NULL
         *
         * @param buf The buf to read object from
         * @return Deserialized object
         * @throws ContextException if the object can't be recovered at this time
         */
        T read(ByteBuf buf) throws ContextException;

    }

    /**
     * Thrown when the object can't be retrieved, because it can't be found in that runtime(context).
     */
    public static class ContextException extends RuntimeException {
        public ContextException(String msg) {
            super(msg);
        }
    }

}
