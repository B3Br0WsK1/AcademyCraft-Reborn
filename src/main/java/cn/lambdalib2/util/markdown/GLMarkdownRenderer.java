package cn.lambdalib2.util.markdown;

import cn.lambdalib2.render.font.IFont;
import cn.lambdalib2.render.font.IFont.FontOption;
import cn.lambdalib2.render.font.TrueTypeFont;
import cn.lambdalib2.render.legacy.GL11;
import cn.lambdalib2.render.legacy.LegacyTextures;
import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.Fragmentor;
import cn.lambdalib2.util.HudUtils;
import cn.lambdalib2.util.RenderUtils;
import cn.lambdalib2.util.markdown.MarkdownParser.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Renders markdown in a GUI context. Ported from Scala.
 */
@Environment(EnvType.CLIENT)
public class GLMarkdownRenderer implements MarkdownRenderer {

    // Render parameters

    public final float[] headerPrefixes = { 1.8f, 1.6f, 1.4f, 1.2f, 1.0f };

    public IFont font = TrueTypeFont.defaultFont;
    public IFont boldFont = TrueTypeFont.defaultFont;
    public IFont italicFont = TrueTypeFont.defaultFont;

    public float fontSize = 10.0f;
    public float lineSpacing = 4;
    public float widthLimit = Float.MAX_VALUE;

    public Color textColor = Colors.white();
    public Color refTextColor = Colors.fromHexColor(0xffe1c385);
    public Color refBackgroundColor = Colors.fromFloat(0.5f, 0.5f, 0.5f, 0.4f);

    public void setFonts(IFont _font, IFont _boldFont, IFont _italicFont) {
        font = _font;
        boldFont = _boldFont;
        italicFont = _italicFont;
    }

    protected static class Context {
        float x = 0.0f;
        float y = 0.0f;
        float lastSize = 0.0f;
        boolean lineBegin = true;

        boolean lineHead() {
            return x == 0.0f;
        }
    }

    protected final Context rc = new Context();

    private final List<Runnable> instructions = new ArrayList<>();

    private static class TextInsr implements Runnable {
        float x, y;
        final IFont font;
        final FontOption option;
        final String txt;

        TextInsr(String _txt, float _x, float _y, IFont _font, FontOption _option) {
            x = _x;
            y = _y;
            font = _font;
            option = _option;
            txt = _txt;
        }

        @Override
        public void run() {
            font.draw(txt, x, y, option);
        }
    }

    private void insr(Runnable r) {
        instructions.add(r);
    }

    @Override
    public void onTextContent(String text, Set<Attribute> attr) {
        IFont usedFont = font;
        float usedSize = fontSize;
        boolean listElement = false;
        Color usedColor = textColor;

        for (Attribute a : attr) {
            if (a instanceof ListElement) {
                if (rc.lineBegin) listElement = true;
            } else if (a instanceof Header h) {
                usedSize = fontSize * headerPrefixes[Math.min(h.level(), 5) - 1];
                if (h.level() <= 3) usedFont = boldFont;
            } else if (a instanceof Emphasize) {
                usedFont = italicFont;
            } else if (a instanceof Strong) {
                usedFont = boldFont;
            } else if (a instanceof Reference) {
                usedColor = refTextColor;
                if (rc.lineHead()) rc.x = usedSize;
            }
        }

        FontOption option = new FontOption(usedSize, usedColor);
        IFont f = usedFont;

        List<String> lines = Fragmentor.toMultiline(text, new Fragmentor.IFontSizeProvider() {
            @Override
            public double getCharWidth(int chr) {
                return f.getCharWidth(chr, option);
            }

            @Override
            public double getTextWidth(String str) {
                return f.getTextWidth(str, option);
            }
        }, rc.x, widthLimit);

        if (listElement) {
            float dotSize = usedSize * 0.2f;
            float indent = usedSize * 1.2f;
            rc.x = indent;
            float x = rc.x;
            float y = rc.y;
            float sz = usedSize;
            insr(() -> {
                GL11.glColor4f(1, 1, 1, 1);
                HudUtils.colorRect(x, y + sz / 2 - dotSize / 2, dotSize, dotSize);
            });
            rc.x += dotSize * 2;
        }

        for (int i = 0; i < lines.size(); ++i) {
            String ln = lines.get(i);

            float width = usedFont.getTextWidth(ln, option);

            // Note: widthLimit * 1.2 is a magic number and this is a temporary hack.
            if (i != 0 || width + rc.x > widthLimit * 1.2) {
                newline(true);
            }

            rc.lastSize = option.fontSize;

            insr(new TextInsr(ln, rc.x, rc.y, usedFont, option));
            rc.x += width;
        }
    }

    private void newline(boolean cont) {
        rc.x = 0.0f;
        if (cont) {
            rc.y += rc.lastSize;
        } else {
            rc.y += rc.lastSize + lineSpacing;
            rc.lastSize = 0;
        }

        rc.lineBegin = !cont;
    }

    @Override
    public void onNewline() {
        newline(false);
    }

    @Override
    public void onTag(String name, Map<String, String> attr) {
        if (name.equals("img")) {
            Identifier src = Identifier.parse(attr.get("src"));
            float scale = attr.containsKey("scale") ? Float.parseFloat(attr.get("scale")) : 1;

            float sw, sh;
            if (attr.containsKey("width") && attr.containsKey("height")) {
                sw = Float.parseFloat(attr.get("width")) * scale;
                sh = Float.parseFloat(attr.get("height")) * scale;
            } else {
                sw = LegacyTextures.getSize(src, false) * scale;
                sh = LegacyTextures.getSize(src, true) * scale;
            }

            if (sw + rc.x > widthLimit) {
                float s2 = (widthLimit - rc.x) / sw;
                sw *= s2;
                sh *= s2;
            }

            float x = rc.x;
            float y = sh >= rc.lastSize ? rc.y : rc.y + rc.lastSize - sh;
            float fw = sw, fh = sh;
            insr(() -> {
                RenderUtils.loadTexture(src);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glColor4f(1, 1, 1, 1);
                HudUtils.rect(x, y, fw, fh);
            });

            float newY = rc.y + Math.max(0f, sh - fontSize);
            // Take all contents rendered in same line previously and alter their position
            for (int i = instructions.size() - 2; i >= 0; --i) {
                if (!(instructions.get(i) instanceof TextInsr t)) break;
                if (t.y == rc.y) t.y = newY;
            }

            rc.x += sw;
            rc.y = newY;
            rc.lastSize = fontSize;
        }
    }

    public void render() {
        for (Runnable r : instructions) r.run();
    }

    public float getMaxHeight() {
        return rc.y + fontSize;
    }

}
