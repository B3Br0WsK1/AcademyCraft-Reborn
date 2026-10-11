/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of LambdaLib modding library.
* https://github.com/LambdaInnovation/LambdaLib
* Licensed under MIT, see project root for more information.
*/
package cn.lambdalib2.cgui;

import cn.lambdalib2.input.Keys;
import cn.lambdalib2.render.gui.GuiDraw;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * A Screen that draws a {@link CGui}.
 */
@Environment(EnvType.CLIENT)
public class CGuiScreen extends Screen {
    protected CGui gui;

    /**
     * Whether the black background should be drawed.
     */
    protected boolean drawBack = true;

    private long pressStartTime = -1;

    public CGuiScreen(CGui _gui) {
        super(Component.empty());
        gui = _gui;
    }

    public CGuiScreen() {
        this(new CGui());
    }

    /**
     * Set whether the black background should be drawed.
     */
    public CGuiScreen setDrawBack(boolean flag) {
        drawBack = flag;
        return this;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (drawBack) {
            super.extractBackground(graphics, mouseX, mouseY, a);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mx, int my, float partial) {
        gui.resize(width, height);
        GuiDraw.begin(graphics);
        try {
            drawScreen(mx, my, partial);
        } finally {
            GuiDraw.end();
        }
        super.extractRenderState(graphics, mx, my, partial);
    }

    /**
     * Draws the screen contents within a GuiDraw context. Overridable, call super to draw the CGui.
     */
    protected void drawScreen(int mx, int my, float partial) {
        gui.draw(mx, my);
    }

    /**
     * Converts a vanilla mouse button to the CGui button id (0 = left, 1 = right, 2 = middle).
     */
    public static int toCGuiButton(int vanillaButton) {
        int id = Keys.fromVanillaMouseButton(vanillaButton);
        if (id == Keys.MOUSE_LEFT) return 0;
        if (id == Keys.MOUSE_RIGHT) return 1;
        if (id == Keys.MOUSE_MIDDLE) return 2;
        return id + 100;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        pressStartTime = System.currentTimeMillis();
        if (gui.mouseClicked((int) event.x(), (int) event.y(), toCGuiButton(event.button()))) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        long dt = pressStartTime == -1 ? 0 : System.currentTimeMillis() - pressStartTime;
        if (gui.mouseClickMove((int) event.x(), (int) event.y(), toCGuiButton(event.button()), dt)) {
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        pressStartTime = -1;
        return super.mouseReleased(event);
    }

    /**
     * In 26.x SDL only delivers character input while text input is started, so CGUI screens (whose text boxes are
     *  not vanilla widgets) enable it for their whole lifetime.
     */
    @Override
    public void added() {
        super.added();
        net.minecraft.client.Minecraft.getInstance().textInputManager().startTextInput(this);
    }

    @Override
    public void removed() {
        net.minecraft.client.Minecraft.getInstance().textInputManager().stopTextInput(this);
        gui.dispose();
        super.removed();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() != Keys.KEY_ESCAPE) {
            gui.keyTyped('\0', event.key());
            cn.lambdalib2.cgui.component.TextBox box =
                gui.focus == null ? null : gui.focus.getComponent(cn.lambdalib2.cgui.component.TextBox.class);
            if (box != null && box.allowEdit) {
                return true; // Text being edited: don't let vanilla handle the key (focus navigation etc.)
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        int cp = event.codepoint();
        if (Character.isBmpCodePoint(cp)) {
            gui.keyTyped((char) cp, Keys.KEY_NONE);
        }
        return true;
    }

    public CGui getGui() {
        return gui;
    }

}
