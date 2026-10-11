package cn.lambdalib2.render.legacy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Stand-in of the 1.12 RenderManager, passed to {@link LegacyRender} constructors. Only exposes the camera rotation.
 */
@Environment(EnvType.CLIENT)
public final class RenderManager {

    public static final RenderManager INSTANCE = new RenderManager();

    public float playerViewX, playerViewY;
    public double viewerPosX, viewerPosY, viewerPosZ;

    private RenderManager() {}

}
