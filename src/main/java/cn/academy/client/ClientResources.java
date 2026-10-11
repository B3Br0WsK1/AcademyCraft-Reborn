package cn.academy.client;

import cn.academy.AcademyCraft;
import cn.academy.Resources;
import cn.lambdalib2.cgui.component.TextBox;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.render.font.Fonts;
import cn.lambdalib2.render.font.IFont;
import cn.lambdalib2.render.font.TrueTypeFont;
import cn.lambdalib2.render.gui.GuiDraw;
import cn.lambdalib2.render.obj.ObjLegacyRender;
import cn.lambdalib2.render.obj.ObjParser;
import cn.lambdalib2.util.Configuration;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;

import java.awt.Font;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A delegation for client resources loading. Should not refer to explicitly.
 * @see Resources
 */
@Environment(EnvType.CLIENT)
public class ClientResources {

    private static boolean fontsInit = false;
    private static TrueTypeFont font, fontBold, fontItalic;

    private static final Map<Identifier, ObjLegacyRender> cachedModels = new HashMap<>();

    private static final IFont lazyFont = new LazyFont(() -> { checkFontInit(); return font; });
    private static final IFont lazyFontBold = new LazyFont(() -> { checkFontInit(); return fontBold; });
    private static final IFont lazyFontItalic = new LazyFont(() -> { checkFontInit(); return fontItalic; });

    public static IFont font() {
        return lazyFont;
    }

    public static IFont fontBold() {
        return lazyFontBold;
    }

    public static IFont fontItalic() {
        return lazyFontItalic;
    }

    /**
     * Textures were previously loaded with generated mipmaps; now they are drawn with linear filtering.
     */
    public static Identifier preloadMipmapTexture(String loc) {
        Identifier ret = Resources.getTexture(loc);
        GuiDraw.markLinear(ret);
        return ret;
    }

    public static Identifier preloadTexture(String loc) {
        return Resources.getTexture(loc);
    }

    public static TextBox newTextBox() {
        TextBox ret = new TextBox();
        ret.font = font();
        return ret;
    }

    public static TextBox newTextBox(IFont.FontOption option) {
        TextBox ret = new TextBox(option);
        ret.font = font();
        return ret;
    }

    public static synchronized ObjLegacyRender getModel(String mdlName) {
        return cachedModels.computeIfAbsent(
            Identifier.fromNamespaceAndPath("academy", "models_obj/" + mdlName + ".obj"),
            (loc) -> new ObjLegacyRender(ObjParser.parse(loc))
        );
    }

    private static void checkFontInit() {
        if (!fontsInit) {
            fontsInit = true;

            Configuration config = AcademyCraft.config;
            String userSpecified = config.getString("font", "gui", "Microsoft YaHei",
                    "The font to be used. If not found in the system, default fonts will be used.");

            font = TrueTypeFont.withFallback(
                Font.PLAIN, 24, userSpecified,
                "微软雅黑",
                "Microsoft YaHei",
                "SimHei",
                "Adobe Heiti Std R"
            );
            fontBold = new TrueTypeFont(font.font.deriveFont(Font.BOLD));
            fontItalic = new TrueTypeFont(font.font.deriveFont(Font.ITALIC));
        }
    }

    @StateEventCallback
    private static void __preInit(StateEvents.PreInit event) {
        Fonts.register("AC_Normal", font());
        Fonts.register("AC_Bold", fontBold());
        Fonts.register("AC_Italic", fontItalic());
    }

    /**
     * Creates the real font on first use, since creating textures needs the render device.
     */
    private static final class LazyFont implements IFont {
        private final Supplier<IFont> supplier;

        LazyFont(Supplier<IFont> supplier) {
            this.supplier = supplier;
        }

        @Override
        public void draw(String str, float x, float y, FontOption option) {
            supplier.get().draw(str, x, y, option);
        }

        @Override
        public float getCharWidth(int chr, FontOption option) {
            return supplier.get().getCharWidth(chr, option);
        }

        @Override
        public float getTextWidth(String str, FontOption option) {
            return supplier.get().getTextWidth(str, option);
        }
    }
}
