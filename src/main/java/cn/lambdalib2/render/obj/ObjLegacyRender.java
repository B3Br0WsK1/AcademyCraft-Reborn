package cn.lambdalib2.render.obj;

import cn.lambdalib2.render.legacy.GL11;
import cn.lambdalib2.render.legacy.LegacyGL;
import cn.lambdalib2.render.obj.ObjModel.Face;
import cn.lambdalib2.render.obj.ObjModel.Vertex;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Collection;

/**
 * Renders an {@link ObjModel} into a vertex consumer using the ENTITY vertex format (position, color, uv, overlay,
 *  light, normal). Triangles are emitted as degenerate quads, since entity pipelines draw quads.
 */
@Environment(EnvType.CLIENT)
public class ObjLegacyRender {
    private final ObjModel _model;

    public ObjLegacyRender(ObjModel model) {
        _model = model;
    }

    public ObjModel getModel() {
        return _model;
    }

    public void renderAll(PoseStack.Pose pose, VertexConsumer vc, int light, int overlay, int argb) {
        for (String k : _model.faces.keySet()) {
            renderPart(k, pose, vc, light, overlay, argb);
        }
    }

    public void renderPart(String part, PoseStack.Pose pose, VertexConsumer vc, int light, int overlay, int argb) {
        Collection<Face> list = _model.faces.get(part);
        for (Face f : list) {
            Vertex v0 = _model.vertices.get(f.i0);
            Vertex v1 = _model.vertices.get(f.i1);
            Vertex v2 = _model.vertices.get(f.i2);

            addVertex(pose, vc, v0, light, overlay, argb);
            addVertex(pose, vc, v1, light, overlay, argb);
            addVertex(pose, vc, v2, light, overlay, argb);
            addVertex(pose, vc, v2, light, overlay, argb);
        }
    }

    /**
     * Renders the whole model through the legacy GL emulation, with the current GL state (texture, color, matrix).
     */
    public void renderAll() {
        for (String k : _model.faces.keySet()) {
            renderPart(k);
        }
    }

    /**
     * Renders a part through the legacy GL emulation, with a simple directional shading.
     */
    public void renderPart(String part) {
        Collection<Face> list = _model.faces.get(part);
        if (list.isEmpty()) return;
        float[] col = LegacyGL.getColor();
        boolean shade = LegacyGL.isWorldActive();
        Matrix4f mat = LegacyGL.currentMatrix();
        Vector3f n = new Vector3f();
        for (Face f : list) {
            Vertex v0 = _model.vertices.get(f.i0);
            Vertex v1 = _model.vertices.get(f.i1);
            Vertex v2 = _model.vertices.get(f.i2);
            if (shade) {
                n.set(v0.normal).add(v1.normal).add(v2.normal);
                mat.transformDirection(n);
                float len = n.length();
                float factor = 1.0f;
                if (len > 1e-6f) {
                    n.div(len);
                    // Approximation of the vanilla entity lighting (two lights from above)
                    factor = 0.55f + 0.45f * Math.max(0, n.y * 0.8f + Math.abs(n.x) * 0.3f + Math.abs(n.z) * 0.2f);
                }
                LegacyGL.color(col[0] * factor, col[1] * factor, col[2] * factor, col[3]);
            }
            LegacyGL.begin(GL11.GL_TRIANGLES);
            legacyVertex(v0);
            legacyVertex(v1);
            legacyVertex(v2);
            LegacyGL.end();
        }
        LegacyGL.color(col[0], col[1], col[2], col[3]);
    }

    private static void legacyVertex(Vertex v) {
        LegacyGL.texCoord(v.uv.x, 1 - v.uv.y);
        LegacyGL.vertex(v.pos.x, v.pos.y, v.pos.z);
    }

    public boolean hasPart(String part) {
        return _model.faces.containsKey(part);
    }

    private void addVertex(PoseStack.Pose pose, VertexConsumer vc, Vertex v, int light, int overlay, int argb) {
        vc.addVertex(pose, v.pos.x, v.pos.y, v.pos.z)
            .setColor(argb)
            .setUv(v.uv.x, 1 - v.uv.y)
            .setOverlay(overlay)
            .setLight(light)
            .setNormal(pose, v.normal.x, v.normal.y, v.normal.z);
    }
}
