package cn.lambdalib2.datapart;

import cn.lambdalib2.event.SubscribeEvent;
import cn.lambdalib2.event.mc.LivingEvents.LivingDeathEvent;
import cn.lambdalib2.event.mc.LivingEvents.LivingUpdateEvent;
import cn.lambdalib2.event.mc.PlayerEvents;
import cn.lambdalib2.s11n.network.NetworkS11n;
import cn.lambdalib2.s11n.network.NetworkS11n.ContextException;
import cn.lambdalib2.s11n.network.NetworkS11n.NetS11nAdaptor;
import cn.lambdalib2.s11n.network.RegNetS11nAdapter;
import cn.lambdalib2.util.Debug;
import cn.lambdalib2.util.Side;
import cn.lambdalib2.util.SideUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class EntityData<Ent extends Entity> {

    public static final String ID = "LL_EntityData";

    private static final List<RegData> regList = new ArrayList<>();
    private static final List<RegData> bothSideList = new ArrayList<>();
    private static final Map<Class<? extends Entity>, Boolean> neededEntityMap = new HashMap<>();

    private static boolean _baked;

    /**
     * Register a DataPart type.
     */
    @SuppressWarnings("unchecked")
    static <T extends Entity> void register(
        Class<? extends DataPart<T>> type,
        EnumSet<Side> sides,
        Predicate<Class<? extends T>> pred) {
        Debug.assert2(!_baked, "Can't register DataPart type after EntityData is used");

        RegData add = new RegData();
        add.type = (Class) type;
        add.sides = EnumSet.copyOf(sides);
        add.pred = (Predicate) pred;

        regList.add(add);
        if (add.sides.contains(Side.CLIENT) && add.sides.contains(Side.SERVER)) {
            bothSideList.add(add);
        }
    }

    public static synchronized boolean needEntityDataFor(Class<? extends Entity> type) {
        if (!_baked) return false;
        Boolean cached = neededEntityMap.get(type);
        if (cached != null) {
            return cached;
        }
        boolean need = false;
        for (RegData data : regList) {
            if (data.pred.test(type)) {
                need = true;
                break;
            }
        }

        neededEntityMap.put(type, need);
        return need;
    }

    static void bake() {
        bothSideList.sort(Comparator.comparing(lhs -> lhs.type.getName()));
        Preconditions.checkState(bothSideList.size() < Byte.MAX_VALUE);
        IntStream.range(0, bothSideList.size()).forEach(i -> bothSideList.get(i).networkID = (byte) i);

        Debug.log("EntityData baked, network participants: " +
            bothSideList.stream().map(x -> x.type.getName()).collect(Collectors.toList()));

        _baked = true;
    }

    public static boolean isBaked() {
        return _baked;
    }

    /**
     * Get entity's EntityData. By get it you can get other data by getPart().
     * @param entity can't be null.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Entity> EntityData<T> get(T entity) {
        Objects.requireNonNull(entity);
        if (!needEntityDataFor(entity.getClass()))
            return null;

        IEntityDataHolder holder = (IEntityDataHolder) entity;
        EntityData<T> ret = (EntityData<T>) holder.lambdalib2$getEntityData();
        if (ret == null) {
            ret = new EntityData<>(entity);
            holder.lambdalib2$setEntityData(ret);
        }

        ret.checkInit();

        return ret;
    }

    /**
     * @return The EntityData if it is already constructed, without creating it.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Entity> EntityData<T> getIfPresent(T entity) {
        return (EntityData<T>) ((IEntityDataHolder) entity).lambdalib2$getEntityData();
    }

    // ---------------------------------------------------------

    private final ImmutableMap<Class, DataPart> constructed;

    private Ent entity;

    private boolean _init = false;

    @SuppressWarnings("unchecked")
    public EntityData(Ent entity) {
        Debug.assertNotNull(entity);
        this.entity = entity;

        Side side = entity.level() != null && entity.level().isClientSide() ? Side.CLIENT : Side.SERVER;

        // Construct all DataParts
        Map<Class, DataPart> map = new HashMap<>();
        for(RegData data : regList) {
            if (data.isApplicable(entity, side)) {
                try {
                    DataPart instance = (DataPart) data.type.getDeclaredConstructor().newInstance();
                    instance.entityData = this;
                    map.put(data.type, instance);
                } catch (ReflectiveOperationException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        constructed = ImmutableMap.copyOf(map);
    }

    private void checkInit() {
        if (_init)
            return;

        _init = true;
        for (DataPart dp : constructed.values()) {
            dp.wake();
        }
    }

    public boolean isInitialized() {
        return entity != null;
    }

    /**
     * @return The datapart of exact type
     * @throws RuntimeException if no such DataPart was registered before
     */
    @SuppressWarnings("unchecked")
    public <T extends DataPart<?>>
    T getPart(Class<T> type) {
        return Debug.assertNotNull(
            (T) constructed.get(type),
            () -> "No DataPart of type " + type + " in " + this
        );
    }

    /**
     * @return The datapart of exact type, or null if not present
     */
    @SuppressWarnings("unchecked")
    public <T extends DataPart<?>> T getPartNonCreate(Class<T> type) {
        return (T) constructed.getOrDefault(type, null);
    }

    public Ent getEntity() {
        return entity;
    }

    public void writeNBT(CompoundTag tag_) {
        CompoundTag tag = new CompoundTag();
        constructed.values().forEach(part -> {
            if (part.needNBTStorage) {
                CompoundTag partTag = new CompoundTag();
                part.toNBT(partTag);
                tag.put(_partNBTID(part), partTag);
            }
        });
        tag_.put(ID, tag);
    }

    public void readNBT(CompoundTag tag_) {
        CompoundTag tag = tag_.getCompoundOrEmpty(ID);
        for (DataPart dp : constructed.values()) {
            if (dp.needNBTStorage) {
                String id = _partNBTID(dp);
                if (tag.contains(id)) {
                    dp.fromNBT(tag.getCompoundOrEmpty(id));
                }
            }
        }
    }

    private String _partNBTID(DataPart part) {
        // Keep the same id format as the original mod (canonical name of the DataPart class)
        return part.getClass().getName().replace('$', '.');
    }

    private static byte getNetworkID(Class<? extends DataPart> type) {
        for (RegData data : bothSideList) {
            if (data.type == type) {
                return data.networkID;
            }
        }
        throw new IllegalStateException(type + " isn't registered as both side");
    }

    private static Class<? extends DataPart> getTypeFromID(byte id) {
        return bothSideList.get(id).type;
    }

    void tick() {
        checkInit();
        for (DataPart part : constructed.values()) {
            part.callTick();
        }
    }

    // ---- Hooks, called by mixins / event bridges

    public static void _onEntityTick(Entity entity) {
        EntityData<Entity> data = get(entity);
        if (data != null) {
            data.tick();
        }
    }

    public static void _onEntitySave(Entity entity, CompoundTag out) {
        EntityData<Entity> data = getIfPresent(entity);
        if (data == null && needEntityDataFor(entity.getClass())) {
            data = get(entity);
        }
        if (data != null) {
            data.writeNBT(out);
        }
    }

    public static void _onEntityLoad(Entity entity, CompoundTag in) {
        EntityData<Entity> data = get(entity);
        if (data != null) {
            data.readNBT(in);
        }
    }

    public enum EventListener {
        instance;

        @SubscribeEvent
        public void onLivingDeath(LivingDeathEvent evt) {
            if (evt.getEntityLiving() instanceof Player) {
                EntityData<Player> playerData = EntityData.get((Player) evt.getEntityLiving());
                if (playerData != null) {
                    playerData.constructed.values().forEach(DataPart::onPlayerDead);
                }
            }
        }

        @SubscribeEvent
        @SuppressWarnings("unchecked")
        public void onPlayerClone(PlayerEvents.Clone evt) {
            EntityData<Player> data = EntityData.get(evt.getOriginal());
            if (data != null) {
                CompoundTag nbt = new CompoundTag();
                data.writeNBT(nbt);
                // Keep the DataPart instance, re-serialize the data
                data.entity = evt.getEntityPlayer();
                EntityData<Player> newData = EntityData.get(evt.getEntityPlayer());
                if (newData != null) {
                    newData.readNBT(nbt);
                }
            }
        }
    }

    @RegNetS11nAdapter(EntityData.class)
    public static NetS11nAdaptor<EntityData> adaptor = new NetS11nAdaptor<EntityData>() {
        @Override
        public void write(ByteBuf buf, EntityData obj) {
            NetworkS11n.serializeWithHint(buf, obj.getEntity(), Entity.class);
        }
        @Override
        public EntityData read(ByteBuf buf) throws ContextException {
            Entity living = NetworkS11n.deserializeWithHint(buf, Entity.class);
            if (living != null) {
                EntityData ret = EntityData.get(living);
                if (ret == null) throw new ContextException("Entity has no EntityData");
                return ret;
            } else {
                throw new ContextException("Entity not found");
            }
        }
    };

    @RegNetS11nAdapter(DataPart.class)
    public static NetS11nAdaptor<DataPart> partAdaptor = new NetS11nAdaptor<DataPart>() {
        @Override
        public void write(ByteBuf buf, DataPart obj) {
            NetworkS11n.serializeWithHint(buf, obj.getData(), EntityData.class);
            buf.writeByte(getNetworkID(obj.getClass()));
        }
        @Override
        public DataPart read(ByteBuf buf) throws ContextException {
            EntityData data = NetworkS11n.deserializeWithHint(buf, EntityData.class);
            if (data == null) {
                throw new ContextException("EntityData not found");
            }
            DataPart part = data.getPartNonCreate(getTypeFromID(buf.readByte()));
            if (part == null) {
                throw new ContextException("DataPart not present");
            }
            return part;
        }
    };

    static class RegData {

        Class<? extends DataPart> type;
        EnumSet<Side> sides;
        Predicate<Class<? extends Entity>> pred;

        byte networkID; // Only useful if created in both sides

        boolean isApplicable(Entity ent, Side side) {
            return sides.contains(side) && pred.test(ent.getClass());
        }

    }

}
