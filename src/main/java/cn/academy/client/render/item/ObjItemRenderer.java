package cn.academy.client.render.item;

import cn.academy.Resources;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.render.legacy.GL11;
import cn.lambdalib2.render.legacy.LegacyEntityRenderer;
import cn.lambdalib2.render.legacy.LegacyGL;
import cn.lambdalib2.render.obj.ObjLegacyRender;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.List;
import java.util.function.Consumer;

/**
 * Renders an item with an OBJ model (replacement of the 1.12 TEISR approach). Used from item model definitions:
 * <pre>
 * { "type": "minecraft:special", "base": "...", "model": { "type": "academy:obj", "model": "maghook",
 *   "texture": "academy:textures/models/maghook.png", "scale": 0.01, "rotation": [0, 0, 0], "translation": [0, 0, 0] } }
 * </pre>
 * The transform is applied in the order translation, rotation (X, Y, Z), scale.
 */
@Environment(EnvType.CLIENT)
public class ObjItemRenderer implements NoDataSpecialModelRenderer {

    private final ObjLegacyRender model;
    private final Identifier texture;
    private final float scale;
    private final List<Float> rotation;
    private final List<Float> translation;

    public ObjItemRenderer(ObjLegacyRender model, Identifier texture, float scale, List<Float> rotation, List<Float> translation) {
        this.model = model;
        this.texture = texture;
        this.scale = scale;
        this.rotation = rotation;
        this.translation = translation;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, int overlayCoords,
                       boolean hasFoil, int outlineColor) {
        LegacyGL.beginWorld(poseStack, collector);
        try {
            LegacyGL.setBrightness(LegacyEntityRenderer.brightnessOf(lightCoords));
            GL11.glTranslated(translation.get(0), translation.get(1), translation.get(2));
            GL11.glRotated(rotation.get(0), 1, 0, 0);
            GL11.glRotated(rotation.get(1), 0, 1, 0);
            GL11.glRotated(rotation.get(2), 0, 0, 1);
            GL11.glScaled(scale, scale, scale);
            LegacyGL.bindTexture(texture);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor4d(1, 1, 1, 1);
            model.renderAll();
        } finally {
            LegacyGL.endWorld();
        }
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(0, 0, 0));
        output.accept(new Vector3f(1, 1, 1));
    }

    private static final Codec<List<Float>> VEC3 = Codec.FLOAT.listOf(3, 3);

    public record Unbaked(String model, Identifier texture, float scale, List<Float> rotation, List<Float> translation)
            implements NoDataSpecialModelRenderer.Unbaked {

        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("model").forGetter(Unbaked::model),
            Identifier.CODEC.fieldOf("texture").forGetter(Unbaked::texture),
            Codec.FLOAT.optionalFieldOf("scale", 1f).forGetter(Unbaked::scale),
            VEC3.optionalFieldOf("rotation", List.of(0f, 0f, 0f)).forGetter(Unbaked::rotation),
            VEC3.optionalFieldOf("translation", List.of(0f, 0f, 0f)).forGetter(Unbaked::translation)
        ).apply(i, Unbaked::new));

        @Override
        public SpecialModelRenderer<Void> bake(SpecialModelRenderer.BakingContext context) {
            return new ObjItemRenderer(Resources.getModel(model), texture, scale, rotation, translation);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }

    @StateEventCallback
    private static void init(StateEvents.PreInit ev) {
        SpecialModelRenderers.ID_MAPPER.put(Identifier.fromNamespaceAndPath("academy", "obj"), Unbaked.MAP_CODEC);
    }

}
