package cn.academy.terminal.app.settings;
import cn.academy.AcademyCraft;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.util.Configuration;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;


/**
 * @author WeAthFolD
 */
@Environment(EnvType.CLIENT)
public abstract class IPropertyElement<T extends UIProperty> {
    
    public abstract Widget getWidget(T prop);
    
    public Configuration getConfig() {
        return AcademyCraft.config;
    }
    
}