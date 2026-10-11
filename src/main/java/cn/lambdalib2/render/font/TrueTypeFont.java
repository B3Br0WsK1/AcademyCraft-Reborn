package cn.lambdalib2.render.font;

import cn.lambdalib2.render.gui.GuiDraw;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * A font that renders system TrueType fonts (through AWT) into texture atlases, so that text can be drawn smoothly
 *  at any size and with CJK characters.
 * Created by Paindar on 2017/10/15. Ported to the 26.x GUI renderer.
 */
@Environment(EnvType.CLIENT)
public class TrueTypeFont implements IFont {

    static class CachedChar {
        int ch;
        int width;
        int index;
        float u;
        float v;

        CachedChar(int ch, int w, int i, float u, float v) {
            this.ch = ch;
            this.width = w;
            this.index = i;
            this.u = u;
            this.v = v;
        }
    }

    private static TrueTypeFont _default;

    /**
     * The default font. Lazily created, since it requires the render device.
     */
    public static final IFont defaultFont = new IFont() {
        private IFont get() {
            if (_default == null) {
                _default = withFallback(Font.PLAIN, 32,
                    "Microsoft YaHei", "Adobe Heiti Std R", "STHeiti",
                    "SimHei", "微软雅黑", "黑体",
                    "Consolas", "Monospace", "Arial");
            }
            return _default;
        }

        @Override
        public void draw(String str, float x, float y, FontOption option) {
            get().draw(str, x, y, option);
        }

        @Override
        public float getCharWidth(int chr, FontOption option) {
            return get().getCharWidth(chr, option);
        }

        @Override
        public float getTextWidth(String str, FontOption option) {
            return get().getTextWidth(str, option);
        }
    };

    private static int fontCounter = 0;

    public static TrueTypeFont withFallback(int style, int size, String... fallbackNames) {
        Font[] allfonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
        List<Font> used = new ArrayList<>();
        for (String c : fallbackNames) {
            for (Font ex : allfonts) {
                if (ex.getName().equalsIgnoreCase(c)) {
                    used.add(ex);
                    break;
                }
            }
        }
        return (used.isEmpty()) ? new TrueTypeFont(new Font(null, style, size)) : new TrueTypeFont(new Font(used.get(0).getName(), style, size));
    }

    private static final int TEXTURE_SZ_LIMIT = 2048;

    private final int charSize;
    private final int maxPerCol;
    private final int maxStep;
    private final List<DynamicTexture> textures = new ArrayList<>();
    private final List<Identifier> textureIds = new ArrayList<>();
    private final BitSet dirty = new BitSet();
    private final Map<Integer, CachedChar> lookup = new HashMap<>();
    private int step = 0;
    private final float texStep;
    private final int id;

    public final Font font;

    public TrueTypeFont(Font font) {
        this.font = font;
        this.id = fontCounter++;
        charSize = (int) (font.getSize() * 1.4);
        maxPerCol = TEXTURE_SZ_LIMIT / charSize;
        maxStep = maxPerCol * maxPerCol;
        texStep = (float) charSize / TEXTURE_SZ_LIMIT;
        newTexture();
    }

    private Font resolve(int codePoint) {
        if (font.canDisplay(codePoint)) {
            return font;
        }
        return font;
    }

    @Override
    public void draw(String str, float px, float y, FontOption option) {
        float len = getTextWidth(str, option); // Which will call updateCache()
        for (int i = dirty.nextSetBit(0); i >= 0; i = dirty.nextSetBit(i + 1)) {
            textures.get(i).upload();
        }
        dirty.clear();

        float sz = option.fontSize;
        float scale = option.fontSize / charSize;
        float x = px - len * option.align.lenOffset;

        // Group quads by atlas page
        Map<Integer, List<float[]>> batches = new TreeMap<>();
        int[] cps = str.codePoints().toArray();
        for (int cp : cps) {
            CachedChar info = lookup.get(cp);
            if (info == null) continue;
            batches.computeIfAbsent(info.index, k -> new ArrayList<>())
                .add(new float[] { x, info.u, info.v });
            x += info.width * scale;
        }

        Identifier prevTex = GuiDraw.getTexture();
        boolean prevTexEnabled = GuiDraw.isTextureEnabled();
        GuiDraw.color(option.color);
        GuiDraw.setLinearFilter(true);
        for (Map.Entry<Integer, List<float[]>> e : batches.entrySet()) {
            GuiDraw.bindTexture(textureIds.get(e.getKey()));
            GuiDraw.beginQuads();
            for (float[] q : e.getValue()) {
                float qx = q[0], u = q[1], v = q[2];
                GuiDraw.vertex(qx, y, u, v);
                GuiDraw.vertex(qx, y + sz, u, v + texStep);
                GuiDraw.vertex(qx + sz, y + sz, u + texStep, v + texStep);
                GuiDraw.vertex(qx + sz, y, u + texStep, v);
            }
            GuiDraw.endQuads();
        }
        GuiDraw.setLinearFilter(false);
        GuiDraw.bindTexture(prevTex);
        if (!prevTexEnabled) GuiDraw.disableTexture();
    }

    @Override
    public float getCharWidth(int chr, FontOption option) {
        if (!lookup.containsKey(chr)) {
            writeImage(chr);
        }
        return lookup.get(chr).width * option.fontSize / charSize;
    }

    @Override
    public float getTextWidth(String str, FontOption option) {
        updateCache(str);
        float sum = 0;
        for (int i : str.codePoints().toArray()) {
            sum += lookup.get(i).width;
        }
        return sum * option.fontSize / charSize;
    }

    private void newTexture() {
        Identifier tid = Identifier.fromNamespaceAndPath("lambdalib2", "dynamic/ttf_" + id + "_" + textures.size());
        DynamicTexture tex = new DynamicTexture(tid::toString, TEXTURE_SZ_LIMIT, TEXTURE_SZ_LIMIT, true);
        Minecraft.getInstance().getTextureManager().register(tid, tex);
        textures.add(tex);
        textureIds.add(tid);
        step = 0;
    }

    // Update the cached images to contain the given new characters.
    private void updateCache(String str) {
        Set<Integer> newchars = new HashSet<>();
        for (int i : str.codePoints().toArray()) {
            if (!lookup.containsKey(i))
                newchars.add(i);
        }
        newchars.forEach(this::writeImage);
    }

    // Draw the image into the cached textures at current step position and increment the step by 1.
    private void writeImage(int ch) {
        BufferedImage image = new BufferedImage(charSize, charSize, BufferedImage.TYPE_INT_ARGB);
        int curIndex = textures.size() - 1;
        DynamicTexture curtex = textures.get(curIndex);

        Graphics2D graphics = image.createGraphics();
        Font drawFont = resolve(ch);

        graphics.setFont(drawFont);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        FontMetrics metrics = graphics.getFontMetrics();
        String s = new StringBuilder(2).appendCodePoint(ch).toString();
        int width = metrics.stringWidth(s);
        graphics.setBackground(new java.awt.Color(255, 255, 255, 0));
        graphics.clearRect(0, 0, charSize, charSize);
        graphics.setColor(java.awt.Color.WHITE);
        graphics.drawString(s, 3, 1 + metrics.getAscent());
        graphics.dispose();

        int rasterX = (step % maxPerCol) * charSize;
        int rasterY = (step / maxPerCol) * charSize;

        NativeImage pixels = curtex.getPixels();
        for (int py = 0; py < charSize; ++py) {
            for (int px = 0; px < charSize; ++px) {
                int argb = image.getRGB(px, py);
                int alpha = (argb >>> 24) & 0xFF;
                pixels.setPixel(rasterX + px, rasterY + py, (alpha << 24) | 0xFFFFFF);
            }
        }

        lookup.put(ch, new CachedChar(ch, width, curIndex, (float) rasterX / TEXTURE_SZ_LIMIT,
                (float) rasterY / TEXTURE_SZ_LIMIT));
        dirty.set(curIndex);

        step += 1;
        if (step == maxStep) {
            newTexture();
        }
    }
}
