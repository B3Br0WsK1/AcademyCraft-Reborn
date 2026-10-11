package cn.lambdalib2.render.legacy;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;

/**
 * An entity renderer in the 1.12 style: {@link #doRender} is called with the entity, its camera relative position and
 *  the partial ticks, and draws through the {@link LegacyGL} emulation (with the GL11 facade). The modelview matrix
 *  is at the camera position when doRender is called, as in 1.12.
 */
@Environment(EnvType.CLIENT)
public abstract class LegacyEntityRenderer<T extends Entity> extends EntityRenderer<T, LegacyEntityRenderer.State> {

    public static class State extends EntityRenderState {
        public Entity entity;
        public float partialTicks;
    }

    /**
     * If true, the entity is always rendered regardless of its bounding box (1.12 ignoreFrustumCheck).
     */
    protected boolean ignoreFrustum = true;

    protected LegacyEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.entity = entity;
        state.partialTicks = partialTicks;
    }

    @Override
    protected boolean affectedByCulling(T entity) {
        return !ignoreFrustum;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.entity == null) return;
        double dx = state.x - camera.pos.x, dy = state.y - camera.pos.y, dz = state.z - camera.pos.z;
        poseStack.pushPose();
        // Move back to the camera origin, as 1.12 renderers expect
        poseStack.translate(-dx, -dy, -dz);
        LegacyGL.beginWorld(poseStack, collector);
        LegacyGL.setCamera(camera.yRot, camera.xRot);
        LegacyGL.setCameraPos(camera.pos);
        try {
            LegacyGL.setBrightness(1.0f);
            doRender((T) state.entity, dx, dy, dz, state.partialTicks);
        } finally {
            LegacyGL.endWorld();
            poseStack.popPose();
        }
    }

    /**
     * @return A brightness factor (0.25~1) from the packed light coords of the render state
     */
    public static float brightnessOf(int packedLight) {
        int l = Math.max(LightCoordsUtil.block(packedLight), LightCoordsUtil.sky(packedLight));
        return 0.25f + 0.75f * l / 15f;
    }

    /**
     * Renders the entity.
     * @param x The entity's x position relative to the camera
     */
    public abstract void doRender(T entity, double x, double y, double z, float partialTicks);

}
