package cn.academy.energy.client.ui;

import cn.academy.Resources;
import cn.academy.block.container.ContainerSolarGen;
import cn.academy.block.tileentity.TileSolarGen;
import cn.academy.core.client.ui.InventoryPage;
import cn.academy.core.client.ui.TechUI;
import cn.academy.core.client.ui.TechUI.ContainerUI;
import cn.academy.core.client.ui.WirelessPage;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.cgui.loader.CGUIDocument;
import cn.lambdalib2.util.HudUtils;
import cn.lambdalib2.util.RenderUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public final class GuiSolarGen {

    private GuiSolarGen() {}

    private static Widget template;
    private static final Identifier texture = Resources.getTexture("guis/effect/effect_solar");

    public static ContainerUI<ContainerSolarGen> apply(ContainerSolarGen container) {
        if (template == null) {
            template = CGUIDocument.read(Resources.getGui("rework/page_solar")).getWidget("main");
        }
        TileSolarGen tile = container.tile;

        Widget main = template.copy();

        Widget animFrame = main.getWidget("ui_block/anim_frame");
        animFrame.listen(FrameEvent.class, (w, e) -> {
            double v = switch (tile.getStatus()) {
                case STOPPED -> 1.0 / 3.0;
                case STRONG -> 0.0;
                case WEAK -> 2.0 / 3.0;
            };
            RenderUtils.loadTexture(texture);
            HudUtils.rawRect(0, 0, 0, v,
                animFrame.transform.width,
                animFrame.transform.height,
                1, 1.0 / 3.0);
        });

        ContainerUI<ContainerSolarGen> ret = new ContainerUI<>(container, InventoryPage.create(main),
            WirelessPage.userPage(tile));

        ret.infoPage
            .histogram(TechUI.histBuffer(tile::getEnergy, tile.bufferSize))
            .seplineInfo()
            .property("gen_speed", () -> String.format("%.2fIF/T", tile.getGeneration(1024)));

        return ret;
    }

}
