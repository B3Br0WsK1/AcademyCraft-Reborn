package cn.lambdalib2.render.legacy;

import cn.lambdalib2.render.gui.GuiDraw;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * An emulation of the fixed function OpenGL immediate mode on top of the deferred 26.x renderer. Code written against
 *  the legacy GL11 API (through {@link GL11}) works in two contexts:
 * <ul>
 *     <li><b>World</b>: between {@link #beginWorld(PoseStack, SubmitNodeCollector)} and {@link #endWorld()}. Geometry
 *     is transformed by the emulated modelview matrix, batched by state and submitted as custom geometry.</li>
 *     <li><b>GUI</b>: whenever a {@link GuiDraw} context is active (and no world context is). The calls are forwarded
 *     to {@link GuiDraw}, matrix operations act on the GUI pose (2D).</li>
 * </ul>
 * Lighting, alpha test, face culling and shaders are ignored (draws are full bright & double sided), which matches
 *  how the effect rendering of the original code looks.
 */
@Environment(EnvType.CLIENT)
public final class LegacyGL {

    private LegacyGL() {}

    // ---- Pipelines & render types

    public enum Blend { NONE, TRANSLUCENT, ADDITIVE }

    private record PipelineKey(boolean textured, Blend blend, boolean depthTest, boolean depthWrite) {}

    private record TypeKey(@Nullable Identifier texture, Blend blend, boolean depthTest, boolean depthWrite) {}

    private static final Map<PipelineKey, RenderPipeline> pipelines = new HashMap<>();
    private static final Map<TypeKey, RenderType> renderTypes = new HashMap<>();

    /**
     * Creates and registers all pipelines. Must be called during client init.
     */
    public static void init() {
        for (boolean textured : new boolean[] { false, true }) {
            for (Blend blend : Blend.values()) {
                for (boolean depthTest : new boolean[] { false, true }) {
                    for (boolean depthWrite : new boolean[] { false, true }) {
                        PipelineKey key = new PipelineKey(textured, blend, depthTest, depthWrite);
                        pipelines.put(key, createPipeline(key));
                    }
                }
            }
        }
    }

    private static RenderPipeline createPipeline(PipelineKey key) {
        String name = "pipeline/legacy_" + (key.textured ? "tex_" : "color_") + key.blend.name().toLowerCase() +
            (key.depthTest ? "_depth" : "_nodepth") + (key.depthWrite ? "_write" : "");
        RenderPipeline.Builder builder = RenderPipeline.builder(
                key.textured ? RenderPipelines.GUI_TEXTURED_SNIPPET : RenderPipelines.GUI_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("lambdalib2", name))
            .withCull(false);
        switch (key.blend) {
            case NONE -> builder.withColorTargetState(ColorTargetState.DEFAULT);
            case TRANSLUCENT -> builder.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT));
            case ADDITIVE -> builder.withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING));
        }
        if (key.depthTest) {
            builder.withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, key.depthWrite));
        } else {
            builder.withDepthStencilState(Optional.empty());
        }
        return RenderPipelines.register(builder.build());
    }

    private static RenderType getRenderType(TypeKey key) {
        return renderTypes.computeIfAbsent(key, k -> {
            RenderPipeline pipeline = pipelines.get(new PipelineKey(k.texture != null, k.blend, k.depthTest, k.depthWrite));
            RenderSetup.RenderSetupBuilder setup = RenderSetup.builder(pipeline);
            if (k.texture != null) {
                setup.withTexture("Sampler0", k.texture, () -> RenderSystem.getSamplerCache()
                    .getSampler(AddressMode.REPEAT, AddressMode.REPEAT, FilterMode.LINEAR, FilterMode.LINEAR, false));
            }
            if (k.blend != Blend.NONE) {
                setup.sortOnUpload();
            }
            return RenderType.create("lambdalib2_legacy", setup.createRenderSetup());
        });
    }

    // ---- State

    private enum Mode { NONE, WORLD, GUI }

    private static @Nullable SubmitNodeCollector collector;
    private static final Deque<Matrix4f> modelView = new ArrayDeque<>();
    private static Matrix4f current = new Matrix4f();
    private static int projectionDepth = 0;
    private static boolean projectionMode = false;

    private static float cr = 1, cg = 1, cb = 1, ca = 1;
    private static @Nullable Identifier texture;
    private static boolean textureEnabled = true;
    private static boolean blendEnabled = false;
    private static int blendSrc = GL11.GL_SRC_ALPHA, blendDst = GL11.GL_ONE_MINUS_SRC_ALPHA;
    private static boolean depthTest = true;
    private static boolean depthMask = true;
    private static float lineWidth = 1;
    private static float brightness = 1;

    // Vertex building
    private static int primitive = -1;
    private static float tu, tv;
    private static final List<float[]> prim = new ArrayList<>(); // x, y, z, u, v, argb(as float bits)

    private static final List<Batch> batches = new ArrayList<>();

    private static final class Batch {
        final TypeKey key;
        final boolean textured;
        float[] data = new float[64 * 6];
        int count;

        Batch(TypeKey key) {
            this.key = key;
            this.textured = key.texture != null;
        }

        void add(float[] v) {
            if ((count + 1) * 6 > data.length) {
                float[] nd = new float[data.length * 2];
                System.arraycopy(data, 0, nd, 0, count * 6);
                data = nd;
            }
            System.arraycopy(v, 0, data, count * 6, 6);
            ++count;
        }

        void emit(VertexConsumer vc) {
            int n = count - count % 4;
            for (int i = 0; i < n; ++i) {
                int o = i * 6;
                VertexConsumer c = vc.addVertex(data[o], data[o + 1], data[o + 2]);
                if (textured) {
                    c.setUv(data[o + 3], data[o + 4]);
                }
                c.setColor(Float.floatToRawIntBits(data[o + 5]));
            }
        }
    }

    private static Mode mode() {
        if (collector != null) return Mode.WORLD;
        if (GuiDraw.isActive()) return Mode.GUI;
        return Mode.NONE;
    }

    /**
     * Starts a world drawing context. The given pose is the initial modelview matrix (camera relative).
     */
    private record SavedContext(SubmitNodeCollector collector, Matrix4f current, Deque<Matrix4f> modelView,
                                List<Batch> batches, float[] color, @Nullable Identifier texture, boolean textureEnabled,
                                boolean blendEnabled, int blendSrc, int blendDst, boolean depthTest, boolean depthMask,
                                float brightness) {}

    private static final Deque<SavedContext> contextStack = new ArrayDeque<>();

    public static void beginWorld(PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        if (collector != null) {
            // Nested context: save the outer one
            contextStack.push(new SavedContext(collector, current, new ArrayDeque<>(modelView), new ArrayList<>(batches),
                new float[] { cr, cg, cb, ca }, texture, textureEnabled, blendEnabled, blendSrc, blendDst, depthTest,
                depthMask, brightness));
            batches.clear();
        }
        collector = submitNodeCollector;
        modelView.clear();
        current = new Matrix4f(poseStack.last().pose());
        projectionDepth = 0;
        projectionMode = false;
        resetState();
    }

    public static void endWorld() {
        flush();
        collector = null;
        modelView.clear();
        current = new Matrix4f();
        if (!contextStack.isEmpty()) {
            SavedContext c = contextStack.pop();
            collector = c.collector;
            current = c.current;
            modelView.addAll(c.modelView);
            batches.addAll(c.batches);
            cr = c.color[0]; cg = c.color[1]; cb = c.color[2]; ca = c.color[3];
            texture = c.texture;
            textureEnabled = c.textureEnabled;
            blendEnabled = c.blendEnabled;
            blendSrc = c.blendSrc;
            blendDst = c.blendDst;
            depthTest = c.depthTest;
            depthMask = c.depthMask;
            brightness = c.brightness;
        }
    }

    public static boolean isWorldActive() {
        return collector != null;
    }

    private static PoseStack currentPoseStack() {
        flush();
        PoseStack ps = new PoseStack();
        ps.last().pose().set(current);
        return ps;
    }

    /**
     * Submits a block model (unit cube at the local origin) with the current matrix. World mode only.
     */
    public static void submitBlock(net.minecraft.world.level.block.state.BlockState state) {
        if (collector == null) return;
        net.minecraft.client.renderer.block.MovingBlockRenderState mbs =
            new net.minecraft.client.renderer.block.MovingBlockRenderState();
        net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.ZERO;
        mbs.randomSeedPos = pos;
        mbs.blockPos = pos;
        mbs.blockState = state;
        net.minecraft.client.multiplayer.ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
        if (level != null) {
            mbs.biome = level.getBiome(pos);
            mbs.cardinalLighting = level.cardinalLighting();
            mbs.lightEngine = level.getLightEngine();
        }
        collector.submitMovingBlock(currentPoseStack(), mbs, 0);
    }

    /**
     * Submits an item model (centered at the local origin, unit size) with the current matrix. World mode only.
     */
    public static void submitItem(net.minecraft.world.item.ItemStack stack,
                                  net.minecraft.world.item.ItemDisplayContext displayContext) {
        if (collector == null || stack.isEmpty()) return;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        net.minecraft.client.renderer.item.ItemStackRenderState st = new net.minecraft.client.renderer.item.ItemStackRenderState();
        mc.getItemModelResolver().updateForTopItem(st, stack, displayContext, mc.level, null, 0);
        st.submit(currentPoseStack(), collector, 0xF000F0,
            net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0);
    }

    public static void resetState() {
        cr = cg = cb = ca = 1;
        texture = null;
        textureEnabled = true;
        blendEnabled = false;
        blendSrc = GL11.GL_SRC_ALPHA;
        blendDst = GL11.GL_ONE_MINUS_SRC_ALPHA;
        depthTest = true;
        depthMask = true;
        lineWidth = 1;
        brightness = 1;
        primitive = -1;
        prim.clear();
    }

    private static float camYaw, camPitch;
    private static net.minecraft.world.phys.Vec3 camPos = net.minecraft.world.phys.Vec3.ZERO;

    public static void setCameraPos(net.minecraft.world.phys.Vec3 pos) {
        camPos = pos;
        RenderManager.INSTANCE.viewerPosX = pos.x;
        RenderManager.INSTANCE.viewerPosY = pos.y;
        RenderManager.INSTANCE.viewerPosZ = pos.z;
    }

    /**
     * @return The camera position of the current frame.
     */
    public static net.minecraft.world.phys.Vec3 cameraPos() {
        return camPos;
    }

    /**
     * Sets the camera rotation of the current frame (1.12 RenderManager.playerViewY / playerViewX).
     */
    public static void setCamera(float yaw, float pitch) {
        camYaw = yaw;
        camPitch = pitch;
        RenderManager.INSTANCE.playerViewY = yaw;
        RenderManager.INSTANCE.playerViewX = pitch;
    }

    public static float cameraYaw() {
        return camYaw;
    }

    public static float cameraPitch() {
        return camPitch;
    }

    /**
     * Sets a brightness multiplier applied to all vertex colors, used to fake lighting of solid models.
     */
    public static void setBrightness(float b) {
        brightness = b;
    }

    public static float getBrightness() {
        return brightness;
    }

    private static void flush() {
        if (collector == null || batches.isEmpty()) {
            batches.clear();
            return;
        }
        PoseStack identity = new PoseStack();
        for (Batch b : batches) {
            if (b.count < 4) continue;
            RenderType type = getRenderType(b.key);
            collector.submitCustomGeometry(identity, type, (pose, vc) -> b.emit(vc));
        }
        batches.clear();
    }

    // ---- Matrix

    public static void matrixMode(int m) {
        projectionMode = m == GL11.GL_PROJECTION;
    }

    public static void pushMatrix() {
        if (projectionMode) {
            ++projectionDepth;
            return;
        }
        switch (mode()) {
            case WORLD -> modelView.push(new Matrix4f(current));
            case GUI -> GuiDraw.pushMatrix();
            default -> {}
        }
    }

    public static void popMatrix() {
        if (projectionMode) {
            if (projectionDepth > 0) --projectionDepth;
            return;
        }
        switch (mode()) {
            case WORLD -> {
                if (!modelView.isEmpty()) current = modelView.pop();
            }
            case GUI -> GuiDraw.popMatrix();
            default -> {}
        }
    }

    public static void loadIdentity() {
        // Not meaningful in the emulation (the initial matrices are provided by the renderer)
    }

    public static void translate(double x, double y, double z) {
        if (projectionMode) return;
        switch (mode()) {
            case WORLD -> current.translate((float) x, (float) y, (float) z);
            case GUI -> GuiDraw.translate(x, y);
            default -> {}
        }
    }

    public static void scale(double x, double y, double z) {
        if (projectionMode) return;
        switch (mode()) {
            case WORLD -> current.scale((float) x, (float) y, (float) z);
            case GUI -> GuiDraw.scale(x, y);
            default -> {}
        }
    }

    public static void rotate(double angle, double x, double y, double z) {
        if (projectionMode) return;
        switch (mode()) {
            case WORLD -> {
                Vector3f axis = new Vector3f((float) x, (float) y, (float) z);
                if (axis.lengthSquared() < 1e-8f) return;
                axis.normalize();
                current.rotate((float) Math.toRadians(angle), axis);
            }
            case GUI -> {
                // Only rotation around Z is meaningful in 2D
                if (Math.abs(z) > 1e-6 && Math.abs(x) < 1e-6 && Math.abs(y) < 1e-6) {
                    GuiDraw.rotate(z > 0 ? angle : -angle);
                }
            }
            default -> {}
        }
    }

    public static void multMatrix(Matrix4f m) {
        if (mode() == Mode.WORLD && !projectionMode) {
            current.mul(m);
        }
    }

    public static Matrix4f currentMatrix() {
        return current;
    }

    // ---- State

    public static void color(float r, float g, float b, float a) {
        if (LegacyDisplayLists.isRecording()) {
            LegacyDisplayLists.recordColor(r, g, b, a);
            return;
        }
        cr = r; cg = g; cb = b; ca = a;
        if (mode() == Mode.GUI) {
            GuiDraw.color4f(r, g, b, a);
        }
    }

    public static float[] getColor() {
        return new float[] { cr, cg, cb, ca };
    }

    public static void bindTexture(@Nullable Identifier tex) {
        texture = tex;
        if (mode() == Mode.GUI) {
            GuiDraw.bindTexture(tex);
            if (!textureEnabled) GuiDraw.disableTexture();
        }
    }

    public static @Nullable Identifier getTexture() {
        return texture;
    }

    public static void enable(int cap) {
        setCap(cap, true);
    }

    public static void disable(int cap) {
        setCap(cap, false);
    }

    private static void setCap(int cap, boolean v) {
        switch (cap) {
            case GL11.GL_TEXTURE_2D -> {
                textureEnabled = v;
                if (mode() == Mode.GUI) {
                    if (v) GuiDraw.enableTexture(); else GuiDraw.disableTexture();
                }
            }
            case GL11.GL_BLEND -> {
                blendEnabled = v;
                updateGuiBlend();
            }
            case GL11.GL_DEPTH_TEST -> depthTest = v;
            default -> {}
        }
    }

    public static boolean isEnabled(int cap) {
        return switch (cap) {
            case GL11.GL_TEXTURE_2D -> textureEnabled;
            case GL11.GL_BLEND -> blendEnabled;
            case GL11.GL_DEPTH_TEST -> depthTest;
            default -> false;
        };
    }

    public static void blendFunc(int src, int dst) {
        blendSrc = src;
        blendDst = dst;
        updateGuiBlend();
    }

    private static void updateGuiBlend() {
        if (mode() == Mode.GUI) {
            GuiDraw.setBlend(currentBlend() == Blend.ADDITIVE ? GuiDraw.Blend.ADDITIVE : GuiDraw.Blend.NORMAL);
        }
    }

    public static void depthMask(boolean flag) {
        depthMask = flag;
    }

    public static void lineWidth(float w) {
        lineWidth = w;
    }

    private static Blend currentBlend() {
        if (!blendEnabled) return Blend.NONE;
        if (blendDst == GL11.GL_ONE) return Blend.ADDITIVE;
        return Blend.TRANSLUCENT;
    }

    // ---- Immediate mode

    public static void begin(int mode) {
        if (LegacyDisplayLists.isRecording()) {
            LegacyDisplayLists.recordBegin(mode);
            return;
        }
        primitive = mode;
        prim.clear();
    }

    public static void texCoord(double u, double v) {
        if (LegacyDisplayLists.isRecording()) {
            LegacyDisplayLists.recordTexCoord(u, v);
            return;
        }
        tu = (float) u;
        tv = (float) v;
    }

    public static void vertex(double x, double y, double z) {
        if (LegacyDisplayLists.isRecording()) {
            LegacyDisplayLists.recordVertex(x, y, z);
            return;
        }
        if (primitive == -1) return;
        Mode m = mode();
        int argb = GuiDraw.packARGB(cr * brightness, cg * brightness, cb * brightness, ca);
        if (m == Mode.WORLD) {
            Vector4f p = current.transform(new Vector4f((float) x, (float) y, (float) z, 1));
            prim.add(new float[] { p.x, p.y, p.z, tu, tv, Float.intBitsToFloat(argb) });
        } else {
            prim.add(new float[] { (float) x, (float) y, (float) z, tu, tv, Float.intBitsToFloat(argb) });
        }
    }

    public static void end() {
        if (LegacyDisplayLists.isRecording()) {
            LegacyDisplayLists.recordEnd();
            return;
        }
        int p = primitive;
        primitive = -1;
        if (prim.isEmpty()) return;
        List<float[]> quads = toQuads(p, prim);
        prim.clear();
        if (quads.isEmpty()) return;

        switch (mode()) {
            case WORLD -> {
                TypeKey key = new TypeKey(textureEnabled ? texture : null, currentBlend(), depthTest, depthMask);
                Batch b = batches.isEmpty() ? null : batches.get(batches.size() - 1);
                if (b == null || !b.key.equals(key)) {
                    b = new Batch(key);
                    batches.add(b);
                }
                for (float[] v : quads) b.add(v);
            }
            case GUI -> {
                GuiDraw.beginQuads();
                for (float[] v : quads) {
                    GuiDraw.vertex(v[0], v[1], v[3], v[4], Float.floatToRawIntBits(v[5]));
                }
                GuiDraw.endQuads();
            }
            default -> {}
        }
    }

    /**
     * Directly adds a quad (4 vertices, local coordinates) with the current state.
     */
    public static void quad(double[][] pos, double[][] uv) {
        begin(GL11.GL_QUADS);
        for (int i = 0; i < 4; ++i) {
            if (uv != null) texCoord(uv[i][0], uv[i][1]);
            vertex(pos[i][0], pos[i][1], pos[i][2]);
        }
        end();
    }

    private static List<float[]> toQuads(int mode, List<float[]> v) {
        List<float[]> out = new ArrayList<>();
        int n = v.size();
        switch (mode) {
            case GL11.GL_QUADS -> {
                for (int i = 0; i + 3 < n; i += 4) {
                    out.add(v.get(i)); out.add(v.get(i + 1)); out.add(v.get(i + 2)); out.add(v.get(i + 3));
                }
            }
            case GL11.GL_TRIANGLES -> {
                for (int i = 0; i + 2 < n; i += 3) {
                    out.add(v.get(i)); out.add(v.get(i + 1)); out.add(v.get(i + 2)); out.add(v.get(i + 2));
                }
            }
            case GL11.GL_TRIANGLE_FAN, GL11.GL_POLYGON -> {
                for (int i = 1; i + 1 < n; ++i) {
                    out.add(v.get(0)); out.add(v.get(i)); out.add(v.get(i + 1)); out.add(v.get(i + 1));
                }
            }
            case GL11.GL_TRIANGLE_STRIP -> {
                for (int i = 0; i + 2 < n; ++i) {
                    if ((i & 1) == 0) {
                        out.add(v.get(i)); out.add(v.get(i + 1)); out.add(v.get(i + 2)); out.add(v.get(i + 2));
                    } else {
                        out.add(v.get(i + 1)); out.add(v.get(i)); out.add(v.get(i + 2)); out.add(v.get(i + 2));
                    }
                }
            }
            case GL11.GL_QUAD_STRIP -> {
                for (int i = 0; i + 3 < n; i += 2) {
                    out.add(v.get(i)); out.add(v.get(i + 1)); out.add(v.get(i + 3)); out.add(v.get(i + 2));
                }
            }
            case GL11.GL_LINES -> {
                for (int i = 0; i + 1 < n; i += 2) {
                    lineQuads(out, v.get(i), v.get(i + 1));
                }
            }
            case GL11.GL_LINE_STRIP, GL11.GL_LINE_LOOP -> {
                for (int i = 0; i + 1 < n; ++i) {
                    lineQuads(out, v.get(i), v.get(i + 1));
                }
                if (mode == GL11.GL_LINE_LOOP && n > 2) {
                    lineQuads(out, v.get(n - 1), v.get(0));
                }
            }
            default -> {}
        }
        return out;
    }

    /**
     * Approximates a line by two crossing thin quads (world) or one thin quad (gui).
     */
    private static void lineQuads(List<float[]> out, float[] a, float[] b) {
        float dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-6f) return;
        boolean world = mode() == Mode.WORLD;
        float w = world ? 0.0075f * lineWidth : 0.5f * lineWidth;
        Vector3f dir = new Vector3f(dx / len, dy / len, dz / len);
        Vector3f[] sides;
        if (world) {
            Vector3f ref = Math.abs(dir.y) < 0.9f ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
            Vector3f s1 = new Vector3f(dir).cross(ref).normalize().mul(w);
            Vector3f s2 = new Vector3f(dir).cross(s1).normalize().mul(w);
            sides = new Vector3f[] { s1, s2 };
        } else {
            sides = new Vector3f[] { new Vector3f(-dir.y, dir.x, 0).mul(w) };
        }
        for (Vector3f s : sides) {
            out.add(new float[] { a[0] + s.x, a[1] + s.y, a[2] + s.z, a[3], a[4], a[5] });
            out.add(new float[] { a[0] - s.x, a[1] - s.y, a[2] - s.z, a[3], a[4], a[5] });
            out.add(new float[] { b[0] - s.x, b[1] - s.y, b[2] - s.z, b[3], b[4], b[5] });
            out.add(new float[] { b[0] + s.x, b[1] + s.y, b[2] + s.z, b[3], b[4], b[5] });
        }
    }

}
