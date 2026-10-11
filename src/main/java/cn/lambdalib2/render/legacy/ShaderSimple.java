package cn.lambdalib2.render.legacy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Compatibility stub of the LambdaLib2 legacy shader; see {@link LegacyShaderProgram}.
 * @author WeAthFolD
 */
@Environment(EnvType.CLIENT)
public class ShaderSimple extends LegacyShaderProgram {
    private static ShaderSimple instance;

    public static ShaderSimple instance() {
        if(instance == null)
            instance = new ShaderSimple();
        return instance;
    }

    private ShaderSimple() {}
}
