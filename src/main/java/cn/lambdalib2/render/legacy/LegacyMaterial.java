package cn.lambdalib2.render.legacy;

import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.Colors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;

/**
 * @author WeAthFolD
 *
 */
@Environment(EnvType.CLIENT)
public abstract class LegacyMaterial {

    public Identifier mainTexture;
    public Color color = Colors.white();

    public abstract void onRenderStage(RenderStage stage);

    public LegacyMaterial setTexture(Identifier tex) {
        mainTexture = tex;
        return this;
    }

}
