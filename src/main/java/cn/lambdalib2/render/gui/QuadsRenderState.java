package cn.lambdalib2.render.gui;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;

/**
 * A GUI element made of arbitrary quads, in local (pose) coordinates. Vertices are 4-aligned.
 */
@Environment(EnvType.CLIENT)
public final class QuadsRenderState implements GuiElementRenderState {

    private final RenderPipeline pipeline;
    private final TextureSetup textureSetup;
    private final Matrix3x2f pose;
    private final float[] data; // x, y, u, v per vertex
    private final int[] colors;
    private final int vertexCount;
    private final boolean textured;
    private final @Nullable ScreenRectangle scissor;
    private final @Nullable ScreenRectangle bounds;

    public QuadsRenderState(RenderPipeline pipeline, TextureSetup textureSetup, boolean textured, Matrix3x2f pose,
                            float[] data, int[] colors, int vertexCount, @Nullable ScreenRectangle scissor) {
        this.pipeline = pipeline;
        this.textureSetup = textureSetup;
        this.textured = textured;
        this.pose = pose;
        this.data = data;
        this.colors = colors;
        this.vertexCount = vertexCount;
        this.scissor = scissor;
        this.bounds = computeBounds();
    }

    private @Nullable ScreenRectangle computeBounds() {
        if (vertexCount == 0) return null;
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        Vector2f tmp = new Vector2f();
        for (int i = 0; i < vertexCount; ++i) {
            pose.transformPosition(data[i * 4], data[i * 4 + 1], tmp);
            minX = Math.min(minX, tmp.x);
            minY = Math.min(minY, tmp.y);
            maxX = Math.max(maxX, tmp.x);
            maxY = Math.max(maxY, tmp.y);
        }
        int x0 = (int) Math.floor(minX), y0 = (int) Math.floor(minY);
        int x1 = (int) Math.ceil(maxX), y1 = (int) Math.ceil(maxY);
        ScreenRectangle rect = new ScreenRectangle(x0, y0, Math.max(1, x1 - x0), Math.max(1, y1 - y0));
        return scissor != null ? scissor.intersection(rect) : rect;
    }

    @Override
    public void buildVertices(VertexConsumer vertexConsumer) {
        for (int i = 0; i < vertexCount; ++i) {
            VertexConsumer vc = vertexConsumer.addVertexWith2DPose(pose, data[i * 4], data[i * 4 + 1]);
            if (textured) {
                vc.setUv(data[i * 4 + 2], data[i * 4 + 3]);
            }
            vc.setColor(colors[i]);
        }
    }

    @Override
    public RenderPipeline pipeline() {
        return pipeline;
    }

    @Override
    public TextureSetup textureSetup() {
        return textureSetup;
    }

    @Override
    public @Nullable ScreenRectangle scissorArea() {
        return scissor;
    }

    @Override
    public @Nullable ScreenRectangle bounds() {
        return bounds;
    }
}
