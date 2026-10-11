package cn.academy.core.client.ui;

import cn.academy.Resources;
import cn.academy.core.client.ui.TechUI.Page;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.component.DrawTexture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * The inventory page of machine GUIs. Ported from Scala.
 */
@Environment(EnvType.CLIENT)
public final class InventoryPage {

    private InventoryPage() {}

    private static Widget template;

    private static Widget template() {
        if (template == null) {
            template = TechUI.readxml("page_inv").getWidget("main");
        }
        return template;
    }

    public static Page create(String name) {
        Page ret = create(template().copy());
        ret.window().getWidget("ui_block").getComponent(DrawTexture.class).setTex(Resources.getTexture("guis/ui/ui_" + name));
        return ret;
    }

    public static Page create(Widget ret) {
        for (Widget w : ret.getDrawList()) {
            if (w.getName().startsWith("ui_")) {
                TechUI.breathe(w);
            }
        }
        return new Page("inv", ret);
    }

}
