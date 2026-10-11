package cn.academy.terminal.app.settings;
import cn.academy.util.ACI18n;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;


@Environment(EnvType.CLIENT)
public class UIProperty {

    public static class Config extends UIProperty {
        public final String category;
        public final Object defValue;

        public Config(IPropertyElement _element, String _category,
                      String _id, Object _defValue, boolean _singlePlayer) {
            super(_element, _id, _singlePlayer);
            category = _category;
            defValue = _defValue;
        }
    }

    public static class Callback extends UIProperty {

        Runnable action;

        public Callback(IPropertyElement _element, String _id, Runnable _action, boolean _singlePlayer) {
            super(_element, _id, _singlePlayer);
            action = _action;
        }
    }

    public final IPropertyElement element;
    public final String id;
    public final boolean singlePlayer;
    
    public UIProperty(IPropertyElement _element, String _id, boolean _singlePlayer) {
        element = _element;
        id = _id;
        singlePlayer = _singlePlayer;
    }

    @Environment(EnvType.CLIENT)
    public String getDisplayID() {
        return ACI18n.translateFormatted("ac.settings.prop." + id);
    }
    
}