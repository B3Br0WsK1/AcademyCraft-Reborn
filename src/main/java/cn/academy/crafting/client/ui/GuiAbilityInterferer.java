package cn.academy.crafting.client.ui;

import cn.academy.Resources;
import cn.academy.block.container.ContainAbilityInterferer;
import cn.academy.block.tileentity.TileAbilityInterferer;
import cn.academy.core.client.ui.InventoryPage;
import cn.academy.core.client.ui.TechUI;
import cn.academy.core.client.ui.TechUI.ContainerUI;
import cn.academy.core.client.ui.WirelessPage;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.component.Component;
import cn.lambdalib2.cgui.component.DrawTexture;
import cn.lambdalib2.cgui.component.ElementList;
import cn.lambdalib2.cgui.component.TextBox;
import cn.lambdalib2.cgui.component.TextBox.ConfirmInputEvent;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.cgui.event.LeftClickEvent;
import cn.lambdalib2.cgui.event.LostFocusEvent;
import cn.lambdalib2.cgui.loader.CGUIDocument;
import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.MathUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.TreeSet;

import static cn.academy.block.tileentity.TileAbilityInterferer.maxRange;
import static cn.academy.block.tileentity.TileAbilityInterferer.minRange;

/**
 * GUI of the ability interferer. Ported from Scala.
 */
@Environment(EnvType.CLIENT)
public final class GuiAbilityInterferer {

    private GuiAbilityInterferer() {}

    private static Widget template;

    private static final Identifier buttonOn = Resources.getTexture("guis/button/button_switch_on");
    private static final Identifier buttonOff = Resources.getTexture("guis/button/button_switch_off");

    private static class Element extends Component {
        final String playerName;

        Element(String playerName) {
            super("Element");
            this.playerName = playerName;
        }

        @Override
        public Component copy() {
            return new Element(playerName);
        }
    }

    private static class Area extends Component {
        Widget focus;

        Area() {
            super("Area");
        }

        @Override
        public Component copy() {
            return new Area();
        }
    }

    public static ContainerUI<ContainAbilityInterferer> apply(ContainAbilityInterferer container) {
        if (template == null) {
            template = CGUIDocument.read(Resources.getGui("rework/page_interfere")).getWidget("main");
        }
        Widget window = template.copy();
        TileAbilityInterferer tile = container.tile;

        {
            Widget listPanel = window.getWidget("panel_whitelist");
            Widget listArea = listPanel.getWidget("zone_whitelist");

            Widget element = listArea.getWidget("element");
            Area area = new Area();

            listArea.removeWidget("element");
            listArea.addComponent(area);

            Runnable[] updateRef = new Runnable[1];

            java.util.function.Consumer<Collection<String>> update = whitelist -> {
                listArea.removeComponent(ElementList.class);
                area.focus = null;

                ElementList elist = new ElementList();

                for (String name : whitelist) {
                    Widget instance = element.copy();
                    DrawTexture dt = instance.getComponent(DrawTexture.class);
                    dt.color.setAlpha(Colors.f2i(0.7f));

                    instance.getWidget("element_name").getComponent(TextBox.class).content = name;
                    instance.listen(FrameEvent.class, (w, e) ->
                        dt.color.setAlpha(Colors.f2i(area.focus == instance ? 1.0f : 0.7f)));
                    instance.listen(LeftClickEvent.class, (w, e) -> area.focus = instance);
                    instance.addComponent(new Element(name));

                    elist.addWidget(instance);
                }

                listArea.addComponent(elist);
            };

            java.util.function.Consumer<Collection<String>> sendUpdate = whitelist ->
                tile.setWhitelistClient(whitelist, () -> update.accept(whitelist));

            listPanel.getWidget("btn_up").listen(LeftClickEvent.class, (w, e) -> {
                ElementList l = listArea.getComponent(ElementList.class);
                if (l != null) l.progressLast();
            });
            listPanel.getWidget("btn_down").listen(LeftClickEvent.class, (w, e) -> {
                ElementList l = listArea.getComponent(ElementList.class);
                if (l != null) l.progressNext();
            });
            listPanel.getWidget("btn_add").listen(LeftClickEvent.class, (w, e) -> {
                Widget box = new Widget().size(40, 10).pos(50, 5)
                    .addComponent(new DrawTexture(null).setColor(new Color(255, 255, 255, 50)))
                    .addComponent(Resources.newTextBox().allowEdit());

                box.listen(ConfirmInputEvent.class, (w2, e2) -> {
                    String str = box.getComponent(TextBox.class).content;
                    if (!str.isEmpty()) {
                        TreeSet<String> set = new TreeSet<>(tile.whitelist());
                        set.add(str);
                        sendUpdate.accept(set);
                    }
                    box.dispose();
                });
                box.listen(LostFocusEvent.class, (w2, e2) -> box.dispose());
                listPanel.addWidget(box);
                box.gainFocus();
            });
            listPanel.getWidget("btn_remove").listen(LeftClickEvent.class, (w, e) -> {
                Widget focus = listArea.getComponent(Area.class).focus;
                if (focus != null) {
                    String name = focus.getComponent(Element.class).playerName;
                    TreeSet<String> set = new TreeSet<>(tile.whitelist());
                    set.remove(name);
                    sendUpdate.accept(set);
                }
            });

            update.accept(tile.whitelist());
        }

        {
            Widget button = window.getWidget("panel_config/element_switch/element_btn_switch");
            DrawTexture texture = button.getComponent(DrawTexture.class);
            Color color = texture.color;
            boolean[] state = { tile.enabled() };

            java.util.function.Consumer<Boolean> setState = state2 -> {
                state[0] = state2;

                int lum = Colors.f2i(state2 ? 1 : 0.6f);
                color.setRed(lum);
                color.setGreen(lum);
                color.setBlue(lum);

                texture.texture = state2 ? buttonOn : buttonOff;
            };

            setState.accept(state[0]);

            button.listen(LeftClickEvent.class, (w, e) -> {
                boolean target = !state[0];
                tile.setEnabledClient(target, () -> setState.accept(target));
            });
        }

        {
            Widget elemRange = window.getWidget("panel_config/element_range");

            java.util.function.DoubleConsumer updateRange = value ->
                elemRange.getWidget("element_text_range").getComponent(TextBox.class).content = String.valueOf(value);

            updateRange.accept(tile.range());

            for (int delta : new int[] { -10, 10 }) {
                elemRange.getWidget(delta < 0 ? "element_btn_left" : "element_btn_right").listen(LeftClickEvent.class, (w, e) -> {
                    double newValue = MathUtils.clampd(minRange, maxRange, tile.range() + delta);
                    tile.setRangeClient(newValue, () -> updateRange.accept(newValue));
                });
            }
        }

        ContainerUI<ContainAbilityInterferer> ret = new ContainerUI<>(container, InventoryPage.create(window),
            WirelessPage.userPage(tile));
        ret.infoPage.histogram(TechUI.histEnergy(tile::getEnergy, tile.getMaxEnergy()));
        return ret;
    }
}
