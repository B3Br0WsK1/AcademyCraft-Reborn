package cn.lambdalib2.render.font;

import cn.lambdalib2.render.gui.GuiDraw;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * IFont implementation backed by Minecraft's own font renderer.
 */
@Environment(EnvType.CLIENT)
public class VanillaFont implements IFont {

    public static final VanillaFont instance = new VanillaFont();

    private VanillaFont() {}

    @Override
    public void draw(String str, float x, float y, FontOption option) {
        float len = getTextWidth(str, option);
        GuiDraw.text(str, x - len * option.align.lenOffset, y, option.fontSize, option.color.toARGB(), false);
    }

    @Override
    public float getCharWidth(int chr, FontOption option) {
        return (float) GuiDraw.charWidth(chr, option.fontSize);
    }

    @Override
    public float getTextWidth(String str, FontOption option) {
        return (float) GuiDraw.textWidth(str, option.fontSize);
    }
}
