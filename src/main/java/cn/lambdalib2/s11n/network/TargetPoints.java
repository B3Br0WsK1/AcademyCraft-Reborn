package cn.lambdalib2.s11n.network;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;

public class TargetPoints {

    public static final double DEFAULT_RANGE = 16;
    private static final Map<Class<?>, TargetPointConverter> converters = new HashMap<>();

    static {
        addConverter(TargetPoint.class, (TargetPointConverter<TargetPoint>) (object, range) -> object);

        addConverter(Entity.class, (TargetPointConverter<Entity>) (object, range) -> {
            if (range == -1)
                range = DEFAULT_RANGE;
            return new TargetPoint(object.level().dimension(), object.getX(), object.getY(), object.getZ(), range);
        });

        addConverter(BlockEntity.class, (TargetPointConverter<BlockEntity>) (object, range) -> {
            if (range == -1)
                range = DEFAULT_RANGE;
            return new TargetPoint(object.getLevel().dimension(), object.getBlockPos().getX() + 0.5,
                    object.getBlockPos().getY() + 0.5, object.getBlockPos().getZ() + 0.5, range);
        });
    }

    public static void addConverter(Class<?> klass, TargetPointConverter conv) {
        if (converters.containsKey(klass)) {
            throw new RuntimeException("Cannot add multiple TargetPointConverter for class " + klass);
        }
        converters.put(klass, conv);
    }

    public static TargetPointConverter findConverter(Object obj) {
        Class klass = obj.getClass();
        TargetPointConverter conv;
        while (klass != null) {
            if ((conv = converters.get(klass)) != null)
                return conv;
            klass = klass.getSuperclass();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static TargetPoint convert(Object obj, double range) {
        TargetPointConverter conv = findConverter(obj);
        if (conv == null) {
            throw new UnsupportedOperationException("Didn't find TargetPoint converter for " + obj);
        }
        return conv.convert(obj, range);
    }

    public interface TargetPointConverter<T> {
        TargetPoint convert(T object, double range);
    }

}
