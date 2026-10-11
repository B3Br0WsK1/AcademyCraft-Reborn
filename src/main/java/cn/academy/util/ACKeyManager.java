package cn.academy.util;

import cn.academy.AcademyCraft;
import cn.academy.event.ConfigModifyEvent;
import cn.academy.terminal.app.settings.PropertyElements;
import cn.academy.terminal.app.settings.SettingsUI;
import cn.lambdalib2.event.SubscribeEvent;
import cn.lambdalib2.input.KeyHandler;
import cn.lambdalib2.input.KeyManager;
import cn.lambdalib2.util.Configuration;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ACKeyManager extends KeyManager {

    public static KeyManager instance = new ACKeyManager();

    @Override
    protected Configuration getConfig() {
        return AcademyCraft.config;
    }

    @SubscribeEvent
    public void onConfigModified(ConfigModifyEvent event) {
        if (event.property.isIntValue())
            resetBindingKey(event.property.getName(), event.property.getInt());
    }

    @Override
    public void addKeyHandler(String name, String keyDesc, int defKeyID, boolean global, KeyHandler handler) {
        super.addKeyHandler(name, keyDesc, defKeyID, global, handler);
        SettingsUI.addProperty(PropertyElements.KEY, "keys", name, defKeyID, false);
    }

    private ACKeyManager() {}
}
