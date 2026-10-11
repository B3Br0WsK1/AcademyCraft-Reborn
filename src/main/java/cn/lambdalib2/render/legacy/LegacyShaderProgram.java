package cn.lambdalib2.render.legacy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;

/**
 * Legacy shader program. Custom GL programs can't be used with the 26.x renderer; this class only keeps the API so
 *  that legacy code compiles. Using a program has no effect (the emulation draws with its default shaders).
 */
@Environment(EnvType.CLIENT)
public class LegacyShaderProgram {

    private static int nextId = 1;
    private final int id = nextId++;

    public LegacyShaderProgram() {}

    public void linkShader(Identifier location, int type) {}

    public int getProgramID() {
        return id;
    }

    public void useProgram() {}

    public int getUniformLocation(String name) {
        return -1;
    }

    public void compile() {}

    public boolean isValid() {
        return true;
    }

    public static Identifier getShader(String name) {
        return Identifier.fromNamespaceAndPath("lambdalib2", "shaders/" + name);
    }

}
