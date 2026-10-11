/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of LambdaLib modding library.
* https://github.com/LambdaInnovation/LambdaLib
* Licensed under MIT, see project root for more information.
*/
package cn.lambdalib2.util.entityx;

import cn.lambdalib2.util.Debug;
import net.minecraft.world.entity.Entity;

import java.util.*;

@SuppressWarnings({"unchecked", "rawtypes"})
public class EntityX<T extends Entity> {

    Map<String, MotionHandler> motionHandlers = new LinkedHashMap<>();
    Map<Integer, List<EntityCallback>> callbacks = new HashMap<>();

    Map<Class<? extends EntityEvent>, List<EntityEventHandler>> eventHandlers = new HashMap<>();

    final T target;
    boolean updated = false;

    int _localTick = -1;

    public EntityX(T _target) {
        target = _target;
    }

    public void startUpdate() {
        updated = true;
        for(MotionHandler mh : new ArrayList<>(motionHandlers.values())) {
            mh.onStart();
        }
    }

    public void update() {
        Iterator<MotionHandler> iter = motionHandlers.values().iterator();
        List<MotionHandler> toUpdate = new ArrayList<>();
        while(iter.hasNext()) {
            MotionHandler mh = iter.next();
            if(mh.isDead) {
                iter.remove();
            } else if(mh.isActive) {
                toUpdate.add(mh);
            }
        }
        for (MotionHandler mh : toUpdate) {
            mh.onUpdate();
        }

        // HACK: this ensures all EntityCallback get called
        while (_localTick < target.tickCount) {
            ++_localTick;

            List<EntityCallback> ecList = callbacks.remove(_localTick);
            if(ecList != null) {
                for(EntityCallback ec : ecList) {
                    ec.execute(target);
                }
            }
        }
    }

    public void addMotionHandler(MotionHandler mh) {
        if(motionHandlers.containsKey(mh.getID())) {
            Debug.error("Duplicate MotionHandler ID");
            return;
        }

        mh.target = target;
        mh.entityX = this;
        motionHandlers.put(mh.getID(), mh);
        if(updated) {
            mh.onStart();
        }
    }

    public void removeMotionHandlers() {
        motionHandlers.clear();
    }

    public <U extends MotionHandler> U getMotionHandler(String id) {
        return (U) motionHandlers.get(id);
    }

    public <U extends MotionHandler> U getMotionHandler(Class<? extends U> klass) {
        for(MotionHandler mo : motionHandlers.values()) {
            if(klass.isAssignableFrom(mo.getClass()))
                return (U) mo;
        }
        return null;
    }

    public void execute(EntityCallback c) {
        c.execute(target);
    }

    public void executeAfter(EntityCallback c, int ticks) {
        int targetTime = ticks + target.tickCount;
        callbacks.computeIfAbsent(targetTime, k -> new ArrayList<>()).add(c);
    }

    public void postEvent(EntityEvent event) {
        List<EntityEventHandler> list = eventHandlers.get(event.getClass());
        if(list != null) {
            for(EntityEventHandler eeh : list) {
                if(eeh.active)
                    eeh.onEvent(event);
            }
        }
    }

    public void regEventHandler(EntityEventHandler eeh) {
        eventHandlers.computeIfAbsent(eeh.getHandledEvent(), k -> new ArrayList<>()).add(eeh);
    }

}
