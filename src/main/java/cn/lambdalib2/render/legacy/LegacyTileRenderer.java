package cn.lambdalib2.render.legacy;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A block entity renderer in the 1.12 TESR style: {@link #render} is called with the block entity, its camera relative
 *  position and the partial ticks, and draws through the {@link LegacyGL} emulation.
 */
@Environment(EnvType.CLIENT)
public abstract class LegacyTileRenderer<T extends BlockEntity> implements BlockEntityRenderer<T, LegacyTileRenderer.State> {

    public static class State extends BlockEntityRenderState {
        public BlockEntity tile;
        public float partialTicks;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T blockEntity, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.tile = blockEntity;
        state.partialTicks = partialTicks;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.tile == null) return;
        net.minecraft.core.BlockPos bp = state.blockPos;
        double dx = bp.getX() - camera.pos.x, dy = bp.getY() - camera.pos.y, dz = bp.getZ() - camera.pos.z;
        poseStack.pushPose();
        // Move back to the camera origin, as 1.12 renderers expect
        poseStack.translate(-dx, -dy, -dz);
        LegacyGL.beginWorld(poseStack, collector);
        LegacyGL.setCamera(camera.yRot, camera.xRot);
        LegacyGL.setCameraPos(camera.pos);
        try {
            LegacyGL.setBrightness(useLighting() ? LegacyEntityRenderer.brightnessOf(state.lightCoords) : 1.0f);
            render((T) state.tile, dx, dy, dz, state.partialTicks);
        } finally {
            LegacyGL.endWorld();
            poseStack.popPose();
        }
    }

    /**
     * @return Whether the light level at the block should darken the rendering
     */
    protected boolean useLighting() {
        return true;
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    /**
     * Renders the block entity. The modelview matrix is at the camera; (x, y, z) is the block position relative to it.
     */
    public abstract void render(T te, double x, double y, double z, float partialTicks);

}
