package cn.academy.core.client.ui;

import cn.academy.Resources;
import cn.academy.util.LocalHelper;
import cn.lambdalib2.cgui.CGuiScreenContainer;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.component.Component;
import cn.lambdalib2.cgui.component.DrawTexture;
import cn.lambdalib2.cgui.component.ProgressBar;
import cn.lambdalib2.cgui.component.ProgressBar.Direction;
import cn.lambdalib2.cgui.component.TextBox;
import cn.lambdalib2.cgui.component.TextBox.ChangeContentEvent;
import cn.lambdalib2.cgui.component.TextBox.ConfirmInputEvent;
import cn.lambdalib2.cgui.component.Transform.HeightAlign;
import cn.lambdalib2.cgui.component.Transform.WidthAlign;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.cgui.event.LeftClickEvent;
import cn.lambdalib2.cgui.loader.CGUIDocument;
import cn.lambdalib2.render.font.IFont;
import cn.lambdalib2.render.font.IFont.FontAlign;
import cn.lambdalib2.render.font.IFont.FontOption;
import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.HudUtils;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.RenderUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import static cn.lambdalib2.render.legacy.GL11.*;

/**
 * The generic UI style of AcademyCraft machines. Ported from Scala.
 */
@Environment(EnvType.CLIENT)
public final class TechUI {

    private TechUI() {}

    static cn.lambdalib2.cgui.WidgetContainer readxml(String loc) {
        return CGUIDocument.read(Identifier.fromNamespaceAndPath("academy", "guis/rework/" + loc + ".xml"));
    }

    private static Widget pageButtonTemplate;

    private static Widget pageButtonTemplate() {
        if (pageButtonTemplate == null) {
            pageButtonTemplate = readxml("pageselect").getWidget("main");
        }
        return pageButtonTemplate;
    }

    private static final Identifier blendQuadTex = Resources.getTexture("guis/blend_quad");
    private static final Identifier histogramTex = Resources.getTexture("guis/histogram");
    private static final Identifier lineTex = Resources.getTexture("guis/line");

    public static final LocalHelper local = LocalHelper.at("ac.gui.common");
    public static final LocalHelper localSep = local.subPath("sep");
    public static final LocalHelper localHist = local.subPath("hist");
    public static final LocalHelper localProperty = local.subPath("prop");

    public record Page(String id, Widget window) {}

    public static class BlendQuad extends Component {

        public double margin;

        public final Color color = Colors.monoBlend(0.0f, 0.5f);

        public BlendQuad() {
            this(4);
        }

        public BlendQuad(double margin) {
            super("BlendQuad");
            this.margin = margin;

            this.listen(FrameEvent.class, (w, e) -> {
                RenderUtils.loadTexture(blendQuadTex);
                glEnable(GL_TEXTURE_2D);
                Colors.bindToGL(color);

                double x = 0, y = 0, wd = widget.transform.width, ht = widget.transform.height;
                double[] xs = { x - this.margin, x, x + wd, x + wd + this.margin };
                double[] ys = { y - this.margin, y, y + ht, y + ht + this.margin };

                glBegin(GL_QUADS);
                for (int i = 0; i < 3; ++i) {
                    for (int j = 0; j < 3; ++j) {
                        quad(i, j, xs[i], ys[j], xs[i + 1], ys[j + 1]);
                    }
                }
                glEnd();

                glColor4d(1, 1, 1, 1);
                RenderUtils.loadTexture(lineTex);

                double mrg = 3.2;
                HudUtils.rect(-mrg, -8.6, wd + mrg * 2, 12);
                HudUtils.rect(-mrg, ht - 2, wd + mrg * 2, 8);
            });
        }

        private static void quad(int col, int row, double x0, double y0, double x1, double y1) {
            double u = col / 3.0;
            double v = row / 3.0;
            double step = 1.0 / 3.0;

            glTexCoord2d(u, v);
            glVertex2d(x0, y0);

            glTexCoord2d(u, v + step);
            glVertex2d(x0, y1);

            glTexCoord2d(u + step, v + step);
            glVertex2d(x1, y1);

            glTexCoord2d(u + step, v);
            glVertex2d(x1, y0);
        }

        @Override
        public Component copy() {
            return new BlendQuad(margin);
        }

    }

    public static void drawTextBox(String content, FontOption option, float x, float y) {
        drawTextBox(content, option, x, y, Float.MAX_VALUE);
    }

    public static void drawTextBox(String content, FontOption option, float x, float y, float limit) {
        float wmargin = 5;
        float hmargin = 2;

        IFont font = Resources.font();
        IFont.Extent extent = font.drawSeperated_Sim(content, limit, option);

        glColor4f(0, 0, 0, 0.5f);
        HudUtils.colorRect(x - extent.width * option.align.lenOffset, y,
            extent.width + wmargin * 2 + 2, extent.height + hmargin * 2);

        glColor4f(1, 1, 1, 0.8f);
        font.drawSeperated(content, x + wmargin, y + hmargin, limit, option);

        glColor4f(1, 1, 1, 1);
    }

    /**
     * Creates a tech UI with specified pages.
     *
     * @param pages The pages of this UI. Must not be empty.
     */
    public static TechUIWidget create(Page... pages) {
        return new TechUIWidget(pages);
    }

    public static void breathe(Widget widget) {
        DrawTexture tex = widget.getComponent(DrawTexture.class);
        if (tex != null) {
            widget.listen(FrameEvent.class, (w, e) -> tex.color.setAlpha(Colors.f2i(breatheAlpha())));
        }
    }

    /**
     * A global alpha value generator for producing uniform breathing effect.
     */
    public static float breatheAlpha() {
        double time = GameTimer.getTime();
        double sin = (1 + Math.sin(time / 0.8)) * 0.5;

        return (float) (0.675 + sin * 0.175);
    }

    public static class TechUIWidget extends Widget {

        private Page currentPage_;

        public TechUIWidget(Page... pages) {
            size(172, 187);
            centered();

            currentPage_ = pages[0];

            for (int idx = 0; idx < pages.length; ++idx) {
                Page page = pages[idx];
                Widget button = pageButtonTemplate().copy();
                button.walign(WidthAlign.LEFT).halign(HeightAlign.TOP);

                DrawTexture buttonTex = button.getComponent(DrawTexture.class);
                buttonTex.setTex(Resources.getTexture("guis/icons/icon_" + page.id()));
                button.scale(0.7f);
                button.pos(-20, idx * 22);
                button.listen(LeftClickEvent.class, (w, e) -> {
                    for (Page p : pages) p.window().transform.doesDraw = false;
                    page.window().transform.doesDraw = true;
                    currentPage_ = page;
                });
                button.listen(FrameEvent.class, (w, evt) -> {
                    float a1 = (evt.hovering || currentPage_ == page) ? 1.0f : 0.8f;
                    float a2 = currentPage_ == page ? 1.0f : 0.8f;
                    buttonTex.color.setAlpha(Colors.f2i(a1));
                    buttonTex.color.setRed(Colors.f2i(a2));
                    buttonTex.color.setGreen(Colors.f2i(a2));
                    buttonTex.color.setBlue(Colors.f2i(a2));
                });

                page.window().transform.doesDraw = false;

                addWidget(button);
                addWidget(page.window());
            }

            pages[0].window().transform.doesDraw = true;
        }

        public Page currentPage() {
            return currentPage_;
        }
    }

    public record HistElement(String id, Color color, DoubleSupplier value, Supplier<String> desc) {}

    public static HistElement histEnergy(DoubleSupplier energy, double max) {
        Color color = Colors.fromHexColor(0xff25c4ff);
        return new HistElement(localHist.get("energy"), color, () -> energy.getAsDouble() / max,
            () -> String.format("%.0f IF", energy.getAsDouble()));
    }

    public static HistElement histBuffer(DoubleSupplier energy, double max) {
        Color color = Colors.fromHexColor(0xff25f7ff);
        return new HistElement(localHist.get("buffer"), color, () -> energy.getAsDouble() / max,
            () -> String.format("%.0f IF", energy.getAsDouble()));
    }

    public static HistElement histPhaseLiquid(DoubleSupplier amt, double max) {
        Color color = Colors.fromHexColor(0xff7680de);
        return new HistElement(localHist.get("liquid"), color, () -> amt.getAsDouble() / max,
            () -> String.format("%.0f mB", amt.getAsDouble()));
    }

    public static HistElement histCapacity(IntSupplier amt, IntSupplier max) {
        Color color = Colors.fromHexColor(0xffff6c00);
        return new HistElement(localHist.get("capacity"), color, () -> (double) amt.getAsInt() / max.getAsInt(),
            () -> amt.getAsInt() + "/" + max.getAsInt());
    }

    /**
     * A container screen with the TechUI style.
     */
    public static class ContainerUI<T extends AbstractContainerMenu> extends CGuiScreenContainer<T> {

        public final TechUIWidget main;
        public final InfoArea infoPage;

        public ContainerUI(T container, Page... pages) {
            super(container, net.minecraft.client.Minecraft.getInstance().player.getInventory(),
                net.minecraft.network.chat.Component.empty(), new cn.lambdalib2.cgui.CGui(), 176 + 31, 166 + 20);

            main = TechUI.create(pages);
            main.pos(-18, 0);

            infoPage = new InfoArea();
            infoPage.pos(main.transform.width + 7, 5);

            main.addWidget(infoPage);

            gui.addWidget(main);
        }

        public boolean shouldDisplayInventory(Page page) {
            return page.id().equals("inv");
        }

        @Override
        public boolean isSlotActive() {
            return shouldDisplayInventory(main.currentPage());
        }
    }

    /**
     * The info area at the right side of a container UI.
     */
    public static class InfoArea extends Widget {

        private static final float keyLength = 40;

        private static final float expectWidth = 100.0f;
        private float expectHeight = 50.0f;

        private double lastFrameTime = GameTimer.getTime();
        private final double blendStartTime = GameTimer.getTime();

        private float elemY = 10;
        private final List<Widget> elements = new ArrayList<>();

        private final List<DrawTexture> blendTextures = new ArrayList<>();
        private final List<TextBox> blendTexts = new ArrayList<>();
        private final List<ProgressBar> blendBars = new ArrayList<>();

        public InfoArea() {
            addComponent(new BlendQuad());

            this.size(expectWidth, 0);

            this.listen(FrameEvent.class, (w, e) -> {
                double dt = Math.min(GameTimer.getTime() - lastFrameTime, 0.5);
                transform.width = move(dt, transform.width, expectWidth);
                transform.height = move(dt, transform.height, expectHeight);

                float balpha = (float) MathUtils.clampd(0, 1, (GameTimer.getTime() - blendStartTime - 0.3) / 0.3);
                int a = Colors.f2i(balpha);
                for (DrawTexture t : blendTextures) t.color.setAlpha(a);
                for (TextBox t : blendTexts) t.option.color.setAlpha(a);
                for (ProgressBar t : blendBars) t.color.setAlpha(a);

                lastFrameTime = GameTimer.getTime();
            });
        }

        private static float move(double dt, float fr, float to) {
            float max = (float) dt * 500;
            float delta = to - fr;
            return fr + Math.min(max, Math.abs(delta)) * Math.signum(delta);
        }

        private DrawTexture blend(DrawTexture t) {
            blendTextures.add(t);
            return t;
        }

        private TextBox blend(TextBox t) {
            blendTexts.add(t);
            return t;
        }

        private ProgressBar blend(ProgressBar t) {
            blendBars.add(t);
            return t;
        }

        public InfoArea histogram(HistElement... elems) {
            Widget widget = new Widget().size(210, 210).scale(0.4f)
                .addComponent(blend(new DrawTexture(histogramTex)));

            for (int idx = 0; idx < elems.length; ++idx) {
                HistElement elem = elems[idx];
                Widget bar = new Widget().size(16, 120).pos(56 + idx * 40, 78);
                ProgressBar progress = blend(new ProgressBar());
                progress.color = elem.color();
                progress.dir = Direction.UP;

                bar.listen(FrameEvent.class, (w, e) -> {
                    progress.progress = MathUtils.clampd(0.03, 1, elem.value().getAsDouble());
                });
                bar.addComponent(progress);

                widget.addWidget(bar);
            }

            blank(-30);
            element(widget);

            for (HistElement elem : elems) {
                histProperty(elem);
            }

            return this;
        }

        public InfoArea sepline(String id) {
            Widget widget = new Widget(expectWidth - 3, 8).pos(3, 0);
            widget.addComponent(blend(Resources.newTextBox(new FontOption(6, Colors.monoBlend(1, 0.6f))))
                .setContent(localSep.get(id)));

            blank(3);
            element(widget);

            return this;
        }

        public InfoArea seplineInfo() {
            return sepline("info");
        }

        public InfoArea button(String name, Runnable callback) {
            TextBox textBox = Resources.newTextBox(new FontOption(9, FontAlign.CENTER)).setContent(name);
            float len = textBox.font.getTextWidth(name, textBox.option);

            Widget widget = new Widget().walign(WidthAlign.CENTER).size(Math.max(50, len + 5), 8);
            widget.listen(FrameEvent.class, (w, evt) -> {
                float lum = evt.hovering ? 1.0f : 0.8f;
                Color color = textBox.option.color;
                color.setRed(Colors.f2i(lum));
                color.setGreen(Colors.f2i(lum));
                color.setBlue(Colors.f2i(lum));
            });
            widget.listen(LeftClickEvent.class, (w, e) -> callback.run());
            widget.addComponent(blend(textBox));

            return element(widget);
        }

        public InfoArea property(String key, Supplier<?> value) {
            return property(key, value, null, false, true, null);
        }

        public InfoArea property(String key, Supplier<?> value, Consumer<String> editCallback) {
            return property(key, value, editCallback, false, true, null);
        }

        /**
         * @param contentCell A temp hack to get the text component (stored at index 0)
         */
        public InfoArea property(String key, Supplier<?> value, Consumer<String> editCallback,
                                 boolean password, boolean colorChange, TextBox[] contentCell) {
            Color idleColor = Colors.fromHexColor(0xffffffff), editColor = Colors.fromHexColor(0xff2180d8);

            TextBox textBox = blend(Resources.newTextBox(new FontOption(8))).setContent(String.valueOf(value.get()));
            Widget valueArea = new Widget().size(40, 8).halign(HeightAlign.CENTER);

            if (editCallback != null) {
                textBox.allowEdit = true;
                textBox.option.color.setColor(idleColor);
                valueArea.listen(ConfirmInputEvent.class, (w, e) -> {
                    if (colorChange) {
                        textBox.option.color.setColor(idleColor);
                    }
                    editCallback.accept(textBox.content);
                });
                valueArea.listen(ChangeContentEvent.class, (w, e) -> {
                    if (colorChange) {
                        textBox.option.color.setColor(editColor);
                    }
                });

                Widget box0 = box("[").pos(-4, 0), box1 = box("]").pos(valueArea.transform.width + 2, 0);
                box0.transform.doesListenKey = false;
                box1.transform.doesListenKey = false;
                valueArea.addWidget(box0);
                valueArea.addWidget(box1);
            } else {
                valueArea.listen(FrameEvent.class, (w, e) -> textBox.content = String.valueOf(value.get()));
            }

            if (password) {
                textBox.doesEcho = true;
            }

            valueArea.addComponent(textBox);

            kvpair(key, valueArea);

            if (contentCell != null) {
                contentCell[0] = textBox;
            }

            return this;
        }

        private Widget box(String ch) {
            return new Widget().size(10, 8)
                .halign(HeightAlign.CENTER)
                .addComponent(blend(Resources.newTextBox(new FontOption(8)).setContent(ch)));
        }

        private void histProperty(HistElement elem) {
            Widget widget = new Widget(expectWidth - 10, 8).pos(6, 0);

            Widget keyArea = new Widget().pos(4, 0).size(32, 8).halign(HeightAlign.CENTER)
                .addComponent(blend(Resources.newTextBox(new FontOption(8))).setContent(elem.id()));
            Widget icon = new Widget().size(6, 6)
                .halign(HeightAlign.CENTER).pos(-3, .5f)
                .addComponents(blend(new DrawTexture(null).setColor(elem.color())));
            TextBox valueText = blend(Resources.newTextBox(new FontOption(8))).setContent(elem.desc().get());
            Widget valueArea = new Widget().pos(keyLength, 0).size(40, 8).halign(HeightAlign.CENTER)
                .addComponent(valueText);

            valueArea.listen(FrameEvent.class, (w, e) -> valueText.content = elem.desc().get());

            widget.addWidget(keyArea);
            widget.addWidget(icon);
            widget.addWidget(valueArea);

            element(widget);
        }

        private void kvpair(String key, Widget value) {
            Widget widget = new Widget(expectWidth - 10, 8).pos(6, 0);
            Widget keyArea = new Widget().size(40, 8).halign(HeightAlign.CENTER)
                .addComponent(blend(Resources.newTextBox(new FontOption(8))).setContent(localProperty.get(key)));
            value.pos(keyLength, 0);

            widget.addWidget(keyArea);
            widget.addWidget(value);

            element(widget);
        }

        public InfoArea element(Widget elem) {
            elem.transform.y = elemY;
            elemY += elem.transform.height * elem.transform.scale;

            elements.add(elem);
            expectHeight = Math.max(50.0f, elemY + 8);
            addWidget(elem);

            return this;
        }

        public InfoArea blank(double ht) {
            elemY += (float) ht;
            return this;
        }

        public InfoArea reset() {
            for (Widget w : elements) w.dispose();
            elements.clear();
            elemY = 10;
            blendTextures.clear();
            blendTexts.clear();
            blendBars.clear();

            return this;
        }

    }

}
