package cn.lambdalib2.render;

import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Render pipelines provided by LambdaLib2. They reuse vanilla shaders and only differ in blend state.
 */
@Environment(EnvType.CLIENT)
public final class LLPipelines {

    /**
     * Untextured GUI quads with additive (SRC_ALPHA, ONE) blending.
     */
    public static final RenderPipeline GUI_ADDITIVE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("lambdalib2", "pipeline/gui_additive"))
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withCull(false)
            .build());

    /**
     * Textured GUI quads with additive (SRC_ALPHA, ONE) blending.
     */
    public static final RenderPipeline GUI_TEXTURED_ADDITIVE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("lambdalib2", "pipeline/gui_textured_additive"))
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withCull(false)
            .build());

    /**
     * Untextured GUI quads, translucent blending, no face culling.
     */
    public static final RenderPipeline GUI = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("lambdalib2", "pipeline/gui"))
            .withCull(false)
            .build());

    /**
     * Textured GUI quads, translucent blending, no face culling.
     */
    public static final RenderPipeline GUI_TEXTURED = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("lambdalib2", "pipeline/gui_textured"))
            .withCull(false)
            .build());

    private LLPipelines() {}

    /**
     * Forces class loading so the pipelines are registered before the shader manager compiles them.
     */
    public static void init() {}
}
