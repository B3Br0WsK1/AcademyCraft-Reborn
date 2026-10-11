package cn.academy.tutorial;
import cn.academy.client.gui.GuiTutorial.ViewRenderEvent;
import cn.academy.tutorial.ACTutorial.Tag;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;


/**
 * A view group for tutorial. Provides a tag to be clicked on and a group of sub views.
 */
public interface ViewGroup {

    @Environment(EnvType.CLIENT)
    default Widget[] getSubViews() {
        return new Widget[0];
    }

    @Environment(EnvType.CLIENT)
    default Widget withDraw(Runnable action) {
        Widget ret = new Widget();
        ret.listen(ViewRenderEvent.class, (w, e) -> { action.run(); });
        return ret;
    }

    /**
     * @return The test displayed when hovering on the group button.
     */
    default String getDisplayText() {
        return "";
    }

    Tag getTag();

}