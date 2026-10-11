package cn.academy.event;
import cn.lambdalib2.util.Configuration.Property;
import cn.lambdalib2.event.Event;


/**
 * Fired whenever a specific property in AC config has been edited.
 * @author WeAthFolD
 */
public class ConfigModifyEvent extends Event {
    
    public final Property property;
    
    public ConfigModifyEvent(Property _prop) {
        property = _prop;
    }
    
}