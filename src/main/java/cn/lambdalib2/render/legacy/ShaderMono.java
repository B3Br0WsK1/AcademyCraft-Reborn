package cn.lambdalib2.render.legacy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Compatibility stub of the LambdaLib2 legacy shader; see {@link LegacyShaderProgram}.
 * @author WeAthFolD
 */
@Environment(EnvType.CLIENT)
public class ShaderMono extends LegacyShaderProgram {
    private static ShaderMono instance;

    public static ShaderMono instance() {
        if(instance == null)
            instance = new ShaderMono();
        return instance;
    }

    private ShaderMono() {}
}
