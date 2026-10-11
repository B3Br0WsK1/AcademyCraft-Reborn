package cn.academy.client.render.entity;

import cn.academy.entity.EntityBlock;
import cn.lambdalib2.util.MathUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.FallingBlockRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;

/**
 * Renders the block carried by an {@link EntityBlock}, rotated by its yaw/pitch.
 * @author WeAthFolD
 */
@Environment(EnvType.CLIENT)
public class RenderEntityBlock<T extends EntityBlock> extends EntityRenderer<T, RenderEntityBlock.State> {

    public static class State extends FallingBlockRenderState {
        public float yaw, pitch;
        public boolean available;
    }

    public RenderEntityBlock(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.5f;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T e, State state, float pt) {
        super.extractRenderState(e, state, pt);
        state.available = e._blockState != null;
        if (!state.available) return;
        BlockPos pos = e.blockPosition();
        state.movingBlockRenderState.randomSeedPos = pos;
        state.movingBlockRenderState.blockPos = pos;
        state.movingBlockRenderState.blockState = e._blockState;
        if (e.level() instanceof ClientLevel clientLevel) {
            state.movingBlockRenderState.biome = clientLevel.getBiome(pos);
            state.movingBlockRenderState.cardinalLighting = clientLevel.cardinalLighting();
            state.movingBlockRenderState.lightEngine = clientLevel.getLightEngine();
        }
        state.yaw = MathUtils.lerpf(e.lastYaw, e.yaw, pt);
        state.pitch = MathUtils.lerpf(e.lastPitch, e.pitch, pt);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.available || state.movingBlockRenderState.blockState.getRenderShape() != RenderShape.MODEL) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0, 0.5, 0);
        poseStack.rotateDegrees(Axis.YP, state.yaw);
        poseStack.rotateDegrees(Axis.XP, state.pitch);
        poseStack.translate(-0.5, -0.5, -0.5);
        collector.submitMovingBlock(poseStack, state.movingBlockRenderState, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

}
