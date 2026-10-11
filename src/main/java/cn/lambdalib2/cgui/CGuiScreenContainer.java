package cn.lambdalib2.cgui;

import cn.lambdalib2.cgui.component.TextBox;
import cn.lambdalib2.input.Keys;
import cn.lambdalib2.render.gui.GuiDraw;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * A container screen that draws a {@link CGui} beneath the slots.
 */
@Environment(EnvType.CLIENT)
public class CGuiScreenContainer<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    protected CGui gui;

    private long pressStartTime = -1;

    public CGuiScreenContainer(T c, Inventory inv, Component title) {
        this(c, inv, title, new CGui());
    }

    public CGuiScreenContainer(T c, Inventory inv, Component title, CGui _gui) {
        this(c, inv, title, _gui, 176, 166);
    }

    public CGuiScreenContainer(T c, Inventory inv, Component title, CGui _gui, int imageWidth, int imageHeight) {
        super(c, inv, title, imageWidth, imageHeight);
        gui = _gui;
    }

    public CGui getGui() {
        return gui;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        if (isSlotActive()) {
            drawGui(graphics, mouseX, mouseY);
        }
    }

    private void drawGui(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        gui.resize(width, height);
        GuiDraw.begin(graphics);
        try {
            gui.draw(mouseX, mouseY);
        } finally {
            GuiDraw.end();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (isSlotActive()) {
            super.extractRenderState(graphics, mouseX, mouseY, a);
        } else {
            drawGui(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        // The original GUIs draw their own labels
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        pressStartTime = System.currentTimeMillis();
        boolean ret = false;
        if (isSlotActive()) {
            ret = super.mouseClicked(event, doubleClick);
        }
        return gui.mouseClicked((int) event.x(), (int) event.y(), CGuiScreen.toCGuiButton(event.button())) || ret;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        boolean ret = false;
        if (isSlotActive()) {
            ret = super.mouseDragged(event, dx, dy);
        }
        long dt = pressStartTime == -1 ? 0 : System.currentTimeMillis() - pressStartTime;
        return gui.mouseClickMove((int) event.x(), (int) event.y(), CGuiScreen.toCGuiButton(event.button()), dt) || ret;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        pressStartTime = -1;
        if (isSlotActive()) {
            return super.mouseReleased(event);
        }
        return false;
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
        gui.keyTyped('\0', event.key());
        if (containerAcceptsKey(event.key()) || event.key() == Keys.KEY_ESCAPE) {
            return super.keyPressed(event);
        }
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        int cp = event.codepoint();
        if (Character.isBmpCodePoint(cp)) {
            gui.keyTyped((char) cp, Keys.KEY_NONE);
        }
        return true;
    }

    /**
     * @return Whether the inventory itself receives key input. (This should be disabled when you are handling some user input)
     */
    protected boolean containerAcceptsKey(int key) {
        TextBox temp;
        return (gui.focus == null ||
                (temp = gui.focus.getComponent(TextBox.class)) == null ||
                !temp.allowEdit);
    }

    /**
     * @return Whether inventory slots should be renderered this frame.
     */
    public boolean isSlotActive() {
        return true;
    }
}
