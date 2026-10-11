package cn.academy;

import cn.academy.client.ClientResources;
import cn.lambdalib2.cgui.component.TextBox;
import cn.lambdalib2.render.font.IFont;
import cn.lambdalib2.render.font.IFont.FontOption;
import cn.lambdalib2.render.obj.ObjLegacyRender;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * Resources dispatcher.
 * @author WeathFolD
 */
public class Resources {

    public static Identifier
        TEX_COIN_FRONT = res("textures/item/coin_front.png"),
        TEX_COIN_BACK = res("textures/item/coin_back.png");

    public static Identifier
        TEX_EMPTY = getTexture("null");

    public static Identifier
        TEX_GLOW_LINE = getTexture("effects/glow_line");

    public static Identifier
        ARC_SMALL[] = getEffectSeq("arcs", 10);

    /**
     * @return A resources with path `loc` located in `academy` namespace.
     */
    public static Identifier res(String loc) {
        return Identifier.fromNamespaceAndPath("academy", loc);
    }

    public static SoundEvent soundExternal(String loc) {
        return SoundEvent.createVariableRangeEvent(Identifier.parse(loc));
    }

    /**
     * @return The sound event with given path (as written in sounds.json, e.g. "em.railgun").
     */
    public static SoundEvent sound(String loc) {
        return SoundEvent.createVariableRangeEvent(res(loc));
    }

    @Environment(EnvType.CLIENT)
    public static ObjLegacyRender getModel(String mdlName) {
        return ClientResources.getModel(mdlName);
    }

    public static Identifier getTexture(String loc) {
        return res("textures/" + fixTexturePath(loc) + ".png");
    }

    /**
     * Converts 1.12 texture folders (blocks/items) to the 26.x ones (block/item).
     */
    private static String fixTexturePath(String loc) {
        if (loc.startsWith("blocks/")) return "block/" + loc.substring(7);
        if (loc.startsWith("items/")) return "item/" + loc.substring(6);
        return loc;
    }

    /**
     * Get the resource location for the texture path specified. The texture is drawn with linear filtering.
     */
    @Environment(EnvType.CLIENT)
    public static Identifier preloadMipmapTexture(String loc) {
        return ClientResources.preloadMipmapTexture(loc);
    }

    @Environment(EnvType.CLIENT)
    public static Identifier preloadTexture(String loc) {
        return ClientResources.preloadTexture(loc);
    }

    public static Identifier getShader(String loc) {
        return res("shaders/" + loc);
    }

    public static Identifier getGui(String loc) {
        return res("guis/" + loc + ".xml");
    }

    public static Identifier[] getTextureSeq(String loc, int n) {
        Identifier[] ret = new Identifier[n];
        for(int i = 0; i < n; ++i)
            ret[i] = getTexture(loc + i);
        return ret;
    }

    public static Identifier[] getEffectSeq(String effectName, int n) {
        Identifier[] layers = new Identifier[n];
        String baseName = "academy:textures/effects/" + effectName + "/";
        for(int i = 0; i < n; ++i) {
            layers[i] = Identifier.parse(baseName + i + ".png");
        }
        return layers;
    }

    public static Identifier[] getRayTextures(String name) {
        Identifier r1 = Identifier.parse("academy:textures/effects/" + name + "/blend_in.png");
        Identifier r2 = Identifier.parse("academy:textures/effects/" + name + "/tile.png");
        Identifier r3 = Identifier.parse("academy:textures/effects/" + name + "/blend_out.png");
        return new Identifier[] { r1, r2, r3 };
    }

    @Environment(EnvType.CLIENT)
    public static IFont font() {
        return ClientResources.font();
    }

    @Environment(EnvType.CLIENT)
    public static IFont fontBold() {
        return ClientResources.fontBold();
    }

    @Environment(EnvType.CLIENT)
    public static IFont fontItalic() {
        return ClientResources.fontItalic();
    }

    /**
     * @return A text box with AcademyCraft's mod font.
     */
    @Environment(EnvType.CLIENT)
    public static TextBox newTextBox() {
        return ClientResources.newTextBox();
    }

    /**
     * @return A text box with AcademyCraft's mod font.
     */
    @Environment(EnvType.CLIENT)
    public static TextBox newTextBox(FontOption option) {
        return ClientResources.newTextBox(option);
    }

}
