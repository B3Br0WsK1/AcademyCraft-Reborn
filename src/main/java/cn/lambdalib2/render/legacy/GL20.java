package cn.lambdalib2.render.legacy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Stub of LWJGL GL20 for legacy code. Shaders are ignored by the emulation.
 */
@Environment(EnvType.CLIENT)
public final class GL20 {
    private GL20() {}

    public static final int GL_FRAGMENT_SHADER = 0x8B30, GL_VERTEX_SHADER = 0x8B31, GL_CURRENT_PROGRAM = 0x8B8D;

    public static void glUseProgram(int program) {}
    public static void glUniform1i(int location, int v) {}
    public static void glUniform1f(int location, float v) {}
    public static void glUniform2f(int location, float a, float b) {}
    public static void glUniform3f(int location, float a, float b, float c) {}
    public static void glUniform4f(int location, float a, float b, float c, float d) {}
    public static int glGetUniformLocation(int program, CharSequence name) { return -1; }
}
