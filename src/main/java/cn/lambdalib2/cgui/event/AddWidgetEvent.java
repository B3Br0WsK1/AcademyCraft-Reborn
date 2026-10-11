package cn.lambdalib2.cgui.event;

import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Fired on CGui when a new widget is added into it.
 */
@Environment(EnvType.CLIENT)
public class AddWidgetEvent implements GuiEvent {
    
    public final Widget widget;
    
    public AddWidgetEvent(Widget w) {
        widget = w;
    }
    
}
