package cn.lambdalib2.render.legacy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;

/**
 * Most commonly used material that can almost handle all the situations within MC.
 * Supports light disabling, default alpha blending and pure-colored drawing.
 * @author WeAthFolD
 */
@Environment(EnvType.CLIENT)
public class SimpleMaterial extends LegacyMaterial {

    public boolean ignoreLight = false;

    public SimpleMaterial(Identifier _texture) {
        setTexture(_texture);
    }

    public SimpleMaterial setIgnoreLight() {
        ignoreLight = true;
        return this;
    }

    @Override
    public void onRenderStage(RenderStage stage) {
        if(stage == RenderStage.BEFORE_TESSELLATE) {
            LegacyGL.enable(GL11.GL_BLEND);
            LegacyGL.color(color.getRedF(), color.getGreenF(), color.getBlueF(), color.getAlphaF());

            if(mainTexture != null) {
                LegacyGL.bindTexture(mainTexture);
                LegacyGL.enable(GL11.GL_TEXTURE_2D);
            } else {
                LegacyGL.disable(GL11.GL_TEXTURE_2D);
            }
        } else if(stage == RenderStage.END) {
            LegacyGL.enable(GL11.GL_TEXTURE_2D);
        }
    }

}
