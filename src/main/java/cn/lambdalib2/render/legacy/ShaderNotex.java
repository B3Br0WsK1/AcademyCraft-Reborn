package cn.lambdalib2.render.legacy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Compatibility stub of the LambdaLib2 legacy shader; see {@link LegacyShaderProgram}.
 * @author WeAthFolD
 */
@Environment(EnvType.CLIENT)
public class ShaderNotex extends LegacyShaderProgram {
    private static ShaderNotex instance;

    public static ShaderNotex instance() {
        if(instance == null)
            instance = new ShaderNotex();
        return instance;
    }

    private ShaderNotex() {}
}
