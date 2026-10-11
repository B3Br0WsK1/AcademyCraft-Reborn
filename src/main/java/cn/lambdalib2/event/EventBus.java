package cn.lambdalib2.event;

import cn.lambdalib2.util.Debug;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A small reflective event bus with the same usage as Forge's EVENT_BUS: objects (or classes, for static handlers)
 *  are registered and every method annotated with {@link SubscribeEvent} receives events assignable to its
 *  parameter type.
 */
public class EventBus {

    private static final class Listener {
        final Object owner;
        final Class<?> eventType;
        final MethodHandle handle;
        final EventPriority priority;
        final boolean receiveCanceled;
        final String desc;

        Listener(Object owner, Class<?> eventType, MethodHandle handle, EventPriority priority, boolean receiveCanceled, String desc) {
            this.owner = owner;
            this.eventType = eventType;
            this.handle = handle;
            this.priority = priority;
            this.receiveCanceled = receiveCanceled;
            this.desc = desc;
        }
    }

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final Map<Class<?>, List<Listener>> dispatchCache = new ConcurrentHashMap<>();
    private final Set<Object> registered = Collections.newSetFromMap(new IdentityHashMap<>());

    public synchronized void register(Object target) {
        if (!registered.add(target)) {
            return;
        }
        boolean isStatic = target instanceof Class;
        Class<?> type = isStatic ? (Class<?>) target : target.getClass();

        Set<String> visited = new HashSet<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                SubscribeEvent anno = m.getAnnotation(SubscribeEvent.class);
                if (anno == null) continue;
                if (Modifier.isStatic(m.getModifiers()) != isStatic) continue;
                if (m.getParameterCount() != 1 || !Event.class.isAssignableFrom(m.getParameterTypes()[0])) {
                    throw new IllegalArgumentException("Invalid event handler " + m);
                }
                String sig = m.getName() + Arrays.toString(m.getParameterTypes());
                if (!visited.add(sig)) continue; // overridden

                try {
                    m.setAccessible(true);
                    MethodHandle mh = MethodHandles.lookup().unreflect(m);
                    if (!isStatic) {
                        mh = mh.bindTo(target);
                    }
                    listeners.add(new Listener(target, m.getParameterTypes()[0], mh, anno.priority(), anno.receiveCanceled(), m.toString()));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        dispatchCache.clear();
    }

    public synchronized void unregister(Object target) {
        if (!registered.remove(target)) return;
        listeners.removeIf(l -> l.owner == target);
        dispatchCache.clear();
    }

    /**
     * Posts the event.
     * @return Whether the event is canceled.
     */
    public boolean post(Event event) {
        List<Listener> list = dispatchCache.computeIfAbsent(event.getClass(), this::buildDispatch);
        for (Listener l : list) {
            if (event.isCanceled() && !l.receiveCanceled) continue;
            try {
                l.handle.invoke(event);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable t) {
                Debug.error("Exception in event handler " + l.desc, t);
            }
        }
        return event.isCanceled();
    }

    private List<Listener> buildDispatch(Class<?> eventClass) {
        List<Listener> ret = new ArrayList<>();
        for (Listener l : listeners) {
            if (l.eventType.isAssignableFrom(eventClass)) {
                ret.add(l);
            }
        }
        ret.sort(Comparator.comparingInt(l -> l.priority.ordinal()));
        return ret;
    }
}
