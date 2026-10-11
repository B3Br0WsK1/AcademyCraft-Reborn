package cn.lambdalib2.render.gui;

import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.render.legacy.LegacyGL;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;

/**
 * Renders {@link LegacyPipRenderState}s.
 */
@Environment(EnvType.CLIENT)
public class LegacyPipRenderer extends PictureInPictureRenderer<LegacyPipRenderState> {

    @Override
    public Class<LegacyPipRenderState> getRenderStateClass() {
        return LegacyPipRenderState.class;
    }

    @Override
    protected void renderToTexture(LegacyPipRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        LegacyGL.beginWorld(poseStack, collector);
        try {
            state.draw().run();
        } finally {
            LegacyGL.endWorld();
        }
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height / 2.0F;
    }

    @Override
    protected String getTextureLabel() {
        return "lambdalib2 legacy pip";
    }

    @StateEventCallback
    private static void init(StateEvents.Init ev) {
        PictureInPictureRendererRegistry.register(ctx -> new LegacyPipRenderer());
    }

}
