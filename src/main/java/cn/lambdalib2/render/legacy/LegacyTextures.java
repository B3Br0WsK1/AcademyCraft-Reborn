package cn.lambdalib2.render.legacy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Texture queries for the legacy GL emulation.
 */
@Environment(EnvType.CLIENT)
public final class LegacyTextures {

    private LegacyTextures() {}

    /**
     * @return Width (or height) of the given texture in pixels, 1 if unknown.
     */
    public static int getSize(@Nullable Identifier tex, boolean height) {
        if (tex == null) return 1;
        try {
            AbstractTexture t = Minecraft.getInstance().getTextureManager().getTexture(tex);
            return height ? t.getTexture().getHeight(0) : t.getTexture().getWidth(0);
        } catch (Exception e) {
            return 1;
        }
    }

}
