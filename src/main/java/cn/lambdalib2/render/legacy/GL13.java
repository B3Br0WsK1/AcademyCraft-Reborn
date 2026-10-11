package cn.lambdalib2.render.legacy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Stub of LWJGL GL13 for legacy code. Multitexturing isn't supported by the emulation.
 */
@Environment(EnvType.CLIENT)
public final class GL13 {
    private GL13() {}

    public static final int GL_TEXTURE0 = 0x84C0, GL_TEXTURE1 = 0x84C1, GL_TEXTURE2 = 0x84C2;

    public static void glActiveTexture(int texture) {}
}
