package cn.lambdalib2.render.legacy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

/**
 * A 1.12 style entity renderer (net.minecraft.client.renderer.entity.Render). It's not a real 26.x renderer; wrap it
 *  with {@link LegacyRenderAdapter} to register it, or call it from another LegacyRender (composition).
 */
@Environment(EnvType.CLIENT)
public abstract class LegacyRender<T extends Entity> {

    protected final RenderManager renderManager;

    /** Unused, kept for compatibility. */
    protected float shadowOpaque, shadowSize;

    protected LegacyRender(RenderManager renderManager) {
        this.renderManager = renderManager;
    }

    protected LegacyRender() {
        this(RenderManager.INSTANCE);
    }

    /**
     * Renders the entity. The modelview matrix is at the camera position; (x, y, z) is the entity position relative to
     *  the camera.
     */
    public abstract void doRender(T entity, double x, double y, double z, float entityYaw, float partialTicks);

    protected Identifier getEntityTexture(T entity) {
        return null;
    }

    protected void bindTexture(Identifier tex) {
        LegacyGL.bindTexture(tex);
    }

    protected boolean bindEntityTexture(T entity) {
        Identifier tex = getEntityTexture(entity);
        if (tex == null) return false;
        bindTexture(tex);
        return true;
    }

    public RenderManager getRenderManager() {
        return renderManager;
    }

}
