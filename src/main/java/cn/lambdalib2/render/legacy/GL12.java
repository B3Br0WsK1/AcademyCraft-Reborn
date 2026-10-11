package cn.lambdalib2.render.legacy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Stub of LWJGL GL12 for legacy code.
 */
@Environment(EnvType.CLIENT)
public final class GL12 {
    private GL12() {}

    public static final int GL_BGRA = 0x80E1, GL_UNSIGNED_INT_8_8_8_8_REV = 0x8367, GL_CLAMP_TO_EDGE = 0x812F,
        GL_RESCALE_NORMAL = 0x803A;
}
