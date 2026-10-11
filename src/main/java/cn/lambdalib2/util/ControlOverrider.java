package cn.lambdalib2.util;

import cn.lambdalib2.LambdaLib2;
import cn.lambdalib2.LambdaLib2Client;
import cn.lambdalib2.event.SubscribeEvent;
import cn.lambdalib2.event.mc.TickEvent;
import cn.lambdalib2.input.Keys;
import cn.lambdalib2.mixin.client.KeyMappingAccessor;
import cn.lambdalib2.registry.mc.RegEventHandler;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.*;

/**
 * Temporarily removes vanilla key mappings bound to given keys, so that those keys can be used exclusively by
 *  mod logic (e.g. ability keys). Overrides are released while a GUI is open.
 */
@Environment(EnvType.CLIENT)
public class ControlOverrider {

    private static final Map<Integer, List<KeyMapping>> activeOverrides = new HashMap<>();
    private static final Map<String, OverrideGroup> overrideGroups = new HashMap<>();

    private static Map<InputConstants.Key, List<KeyMapping>> completeBackup;
    private static Screen lastTickGui;

    private static Map<InputConstants.Key, List<KeyMapping>> kbMap() {
        return KeyMappingAccessor.lambdalib2$getMap();
    }

    /**
     * A complete override stops ALL minecraft keys to function. Currently you can open up complete override globally only once.
     */
    public static void startCompleteOverride() {
        if (completeBackup == null) {
            completeBackup = new HashMap<>();
            for (Map.Entry<InputConstants.Key, List<KeyMapping>> e : kbMap().entrySet()) {
                completeBackup.put(e.getKey(), new ArrayList<>(e.getValue()));
                for (KeyMapping kb : e.getValue()) {
                    kb.setDown(false);
                }
            }
            kbMap().clear();
        }
    }

    public static void endCompleteOverride() {
        if (completeBackup != null) {
            kbMap().clear();
            kbMap().putAll(completeBackup);
            completeBackup = null;
            // Re-apply active key overrides on the restored map
            rebuild();
        } else {
            throw error("Try to stop complete override while not overriding at all");
        }
    }

    public static boolean isCompleteOverriding() {
        return completeBackup != null;
    }

    /**
     * Activates an override group. The previous group with given name is ended.
     */
    public static void override(String name, int... keys) {
        overrideGroups.put(name, new OverrideGroup(keys));
        rebuild();
    }

    /**
     * Ends an override group.
     */
    public static void endOverride(String name) {
        OverrideGroup group = overrideGroups.get(name);
        if (group != null) {
            group.end();
        }
    }

    private static void rebuild() {
        clearInternal();

        Set<Integer> keys = new HashSet<>();

        Iterator<OverrideGroup> iter = overrideGroups.values().iterator();
        while (iter.hasNext()) {
            OverrideGroup group = iter.next();
            if (group.ended) {
                iter.remove();
            } else {
                for (int i : group.keys) {
                    keys.add(i);
                }
            }
        }

        if (completeBackup != null) {
            return; // Everything is overridden already
        }

        for (int keyid : keys) {
            List<KeyMapping> list = kbMap().remove(Keys.toInputKey(keyid));
            if (list == null)
                continue;
            for (KeyMapping kb : list) {
                kb.setDown(false);
            }
            activeOverrides.put(keyid, list);
            log("Override new [" + keyid + "]");
        }
    }

    private static void clearInternal() {
        if (completeBackup == null) {
            activeOverrides.forEach((keyid, list) -> {
                InputConstants.Key key = Keys.toInputKey(keyid);
                List<KeyMapping> current = kbMap().computeIfAbsent(key, k -> new ArrayList<>());
                for (KeyMapping kb : list) {
                    if (!current.contains(kb)) current.add(kb);
                }
                log("Override remove [" + keyid + "]");
            });
        }

        activeOverrides.clear();
    }

    private static void releaseLocks() {
        if (completeBackup != null) return;
        for (Map.Entry<Integer, List<KeyMapping>> ao : activeOverrides.entrySet()) {
            List<KeyMapping> current = kbMap().computeIfAbsent(Keys.toInputKey(ao.getKey()), k -> new ArrayList<>());
            for (KeyMapping kb : ao.getValue()) {
                if (!current.contains(kb)) current.add(kb);
            }
        }
    }

    private static void restoreLocks() {
        if (completeBackup != null) return;
        for (Map.Entry<Integer, List<KeyMapping>> ao : activeOverrides.entrySet()) {
            List<KeyMapping> current = kbMap().get(Keys.toInputKey(ao.getKey()));
            for (KeyMapping kb : ao.getValue()) {
                kb.setDown(false);
                if (current != null) current.remove(kb);
            }
        }
    }

    private static void log(String s) {
        if (LambdaLib2.DEBUG)
            Debug.log(s);
    }

    private static RuntimeException error(String s) {
        return new RuntimeException("ControlOverrider error: " + s);
    }

    @Environment(EnvType.CLIENT)
    public enum Events {
        @RegEventHandler
        instance_;

        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent cte) {
            if (cte.phase != TickEvent.Phase.START) return;
            Screen cgs = Minecraft.getInstance().gui.screen();
            if (lastTickGui == null && cgs != null) {
                releaseLocks();
            }
            if (lastTickGui != null && cgs == null) {
                restoreLocks();
            }
            lastTickGui = cgs;
        }

        @SubscribeEvent
        public void onDisconnect(cn.lambdalib2.event.mc.ClientDisconnectionEvent evt) {
            clearInternal();
            if (completeBackup != null) {
                endCompleteOverride();
            }
            overrideGroups.clear();
        }
    }

    /**
     * A group of key overrides with lifetime.
     */
    private static final class OverrideGroup {
        private final int[] keys;
        private boolean ended = false;

        public OverrideGroup(int... _keys) {
            keys = _keys;
        }

        public void end() {
            ended = true;
            ControlOverrider.rebuild();
        }
    }
}
