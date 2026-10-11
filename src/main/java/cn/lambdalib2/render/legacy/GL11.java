package cn.lambdalib2.render.legacy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Matrix4f;

import java.nio.FloatBuffer;

/**
 * A drop-in replacement of the LWJGL2 GL11 class used by legacy rendering code. Every call is routed to
 *  {@link LegacyGL}; unsupported functionality is silently ignored.
 */
@Environment(EnvType.CLIENT)
@SuppressWarnings("unused")
public final class GL11 {

    private GL11() {}

    // ---- Constants (same values as OpenGL)
    public static final int GL_POINTS = 0x0, GL_LINES = 0x1, GL_LINE_LOOP = 0x2, GL_LINE_STRIP = 0x3,
        GL_TRIANGLES = 0x4, GL_TRIANGLE_STRIP = 0x5, GL_TRIANGLE_FAN = 0x6, GL_QUADS = 0x7, GL_QUAD_STRIP = 0x8,
        GL_POLYGON = 0x9;

    public static final int GL_ZERO = 0, GL_ONE = 1, GL_SRC_COLOR = 0x300, GL_ONE_MINUS_SRC_COLOR = 0x301,
        GL_SRC_ALPHA = 0x302, GL_ONE_MINUS_SRC_ALPHA = 0x303, GL_DST_ALPHA = 0x304, GL_ONE_MINUS_DST_ALPHA = 0x305,
        GL_DST_COLOR = 0x306, GL_ONE_MINUS_DST_COLOR = 0x307;

    public static final int GL_NEVER = 0x200, GL_LESS = 0x201, GL_EQUAL = 0x202, GL_LEQUAL = 0x203, GL_GREATER = 0x204,
        GL_NOTEQUAL = 0x205, GL_GEQUAL = 0x206, GL_ALWAYS = 0x207;

    public static final int GL_CULL_FACE = 0xB44, GL_LIGHTING = 0xB50, GL_DEPTH_TEST = 0xB71, GL_ALPHA_TEST = 0xBC0,
        GL_BLEND = 0xBE2, GL_TEXTURE_2D = 0xDE1, GL_FOG = 0xB60, GL_NORMALIZE = 0xBA1, GL_RESCALE_NORMAL = 0x803A,
        GL_COLOR_MATERIAL = 0xB57, GL_LINE_SMOOTH = 0xB20, GL_POLYGON_OFFSET_FILL = 0x8037, GL_SCISSOR_TEST = 0xC11,
        GL_STENCIL_TEST = 0xB90, GL_LIGHT0 = 0x4000, GL_LIGHT1 = 0x4001;

    public static final int GL_FRONT = 0x404, GL_BACK = 0x405, GL_FRONT_AND_BACK = 0x408;
    public static final int GL_FLAT = 0x1D00, GL_SMOOTH = 0x1D01;
    public static final int GL_MODELVIEW = 0x1700, GL_PROJECTION = 0x1701, GL_TEXTURE = 0x1702;
    public static final int GL_MODELVIEW_MATRIX = 0xBA6, GL_PROJECTION_MATRIX = 0xBA7;
    public static final int GL_TEXTURE_WIDTH = 0x1000, GL_TEXTURE_HEIGHT = 0x1001, GL_TEXTURE_BINDING_2D = 0x8069;
    public static final int GL_TEXTURE_MIN_FILTER = 0x2801, GL_TEXTURE_MAG_FILTER = 0x2800, GL_LINEAR = 0x2601,
        GL_NEAREST = 0x2600, GL_TEXTURE_WRAP_S = 0x2802, GL_TEXTURE_WRAP_T = 0x2803, GL_REPEAT = 0x2901, GL_CLAMP = 0x2900;
    public static final int GL_COMPILE = 0x1300, GL_COMPILE_AND_EXECUTE = 0x1301;
    public static final int GL_COLOR_BUFFER_BIT = 0x4000, GL_DEPTH_BUFFER_BIT = 0x100, GL_STENCIL_BUFFER_BIT = 0x400;
    public static final int GL_RGBA = 0x1908, GL_UNSIGNED_BYTE = 0x1401, GL_FLOAT = 0x1406;
    public static final int GL_NO_ERROR = 0;
    public static final int GL_CURRENT_COLOR = 0xB00;
    public static final int GL_VIEWPORT = 0xBA2;

    // ---- Matrix

    public static void glMatrixMode(int mode) { LegacyGL.matrixMode(mode); }
    public static void glPushMatrix() { LegacyGL.pushMatrix(); }
    public static void glPopMatrix() { LegacyGL.popMatrix(); }
    public static void glLoadIdentity() { LegacyGL.loadIdentity(); }
    public static void glTranslated(double x, double y, double z) { LegacyGL.translate(x, y, z); }
    public static void glTranslatef(float x, float y, float z) { LegacyGL.translate(x, y, z); }
    public static void glScaled(double x, double y, double z) { LegacyGL.scale(x, y, z); }
    public static void glScalef(float x, float y, float z) { LegacyGL.scale(x, y, z); }
    public static void glRotated(double a, double x, double y, double z) { LegacyGL.rotate(a, x, y, z); }
    public static void glRotatef(float a, float x, float y, float z) { LegacyGL.rotate(a, x, y, z); }
    public static void glMultMatrix(Matrix4f m) { LegacyGL.multMatrix(m); }
    public static void glMultMatrix(FloatBuffer buf) {
        Matrix4f m = new Matrix4f();
        m.set(buf);
        LegacyGL.multMatrix(m);
    }
    public static void glOrtho(double l, double r, double b, double t, double n, double f) {}
    public static void glViewport(int x, int y, int w, int h) {}

    // ---- State

    public static void glEnable(int cap) { LegacyGL.enable(cap); }
    public static void glDisable(int cap) { LegacyGL.disable(cap); }
    public static boolean glIsEnabled(int cap) { return LegacyGL.isEnabled(cap); }
    public static void glBlendFunc(int src, int dst) { LegacyGL.blendFunc(src, dst); }
    public static void glDepthMask(boolean flag) { LegacyGL.depthMask(flag); }
    public static void glDepthFunc(int func) {
        if (func == GL_ALWAYS) {
            LegacyGL.disable(GL_DEPTH_TEST);
        }
    }
    public static void glAlphaFunc(int func, float ref) {}
    public static void glCullFace(int mode) {}
    public static void glShadeModel(int mode) {}
    public static void glLineWidth(float w) { LegacyGL.lineWidth(w); }
    public static void glPointSize(float s) {}
    public static void glColorMask(boolean r, boolean g, boolean b, boolean a) {}
    public static void glPolygonOffset(float factor, float units) {}
    public static void glClear(int mask) {}
    public static void glHint(int target, int mode) {}
    public static void glTexParameteri(int target, int pname, int param) {}
    public static void glTexParameterf(int target, int pname, float param) {}

    public static int glGetError() { return GL_NO_ERROR; }

    public static int glGetInteger(int pname) { return 0; }

    public static void glGetFloat(int pname, FloatBuffer buf) {
        if (pname == GL_CURRENT_COLOR) {
            buf.put(LegacyGL.getColor());
            buf.flip();
        } else if (pname == GL_MODELVIEW_MATRIX) {
            LegacyGL.currentMatrix().get(buf);
        }
    }

    public static int glGetTexLevelParameteri(int target, int level, int pname) {
        return LegacyTextures.getSize(LegacyGL.getTexture(), pname == GL_TEXTURE_HEIGHT);
    }

    public static void glBindTexture(int target, int texture) {}

    // ---- Color

    public static void glColor4d(double r, double g, double b, double a) { LegacyGL.color((float) r, (float) g, (float) b, (float) a); }
    public static void glColor4f(float r, float g, float b, float a) { LegacyGL.color(r, g, b, a); }
    public static void glColor3f(float r, float g, float b) { LegacyGL.color(r, g, b, 1); }
    public static void glColor3d(double r, double g, double b) { LegacyGL.color((float) r, (float) g, (float) b, 1); }
    public static void glColor4ub(byte r, byte g, byte b, byte a) {
        LegacyGL.color((r & 0xFF) / 255f, (g & 0xFF) / 255f, (b & 0xFF) / 255f, (a & 0xFF) / 255f);
    }

    // ---- Immediate mode

    public static void glBegin(int mode) { LegacyGL.begin(mode); }
    public static void glEnd() { LegacyGL.end(); }
    public static void glVertex3d(double x, double y, double z) { LegacyGL.vertex(x, y, z); }
    public static void glVertex3f(float x, float y, float z) { LegacyGL.vertex(x, y, z); }
    public static void glVertex2d(double x, double y) { LegacyGL.vertex(x, y, 0); }
    public static void glVertex2f(float x, float y) { LegacyGL.vertex(x, y, 0); }
    public static void glVertex2i(int x, int y) { LegacyGL.vertex(x, y, 0); }
    public static void glTexCoord2d(double u, double v) { LegacyGL.texCoord(u, v); }
    public static void glTexCoord2f(float u, float v) { LegacyGL.texCoord(u, v); }
    public static void glNormal3d(double x, double y, double z) {}
    public static void glNormal3f(float x, float y, float z) {}

    // ---- Display lists (recorded and replayed through the emulation)

    public static int glGenLists(int range) { return LegacyDisplayLists.genLists(range); }
    public static void glNewList(int list, int mode) { LegacyDisplayLists.newList(list); }
    public static void glEndList() { LegacyDisplayLists.endList(); }
    public static void glCallList(int list) { LegacyDisplayLists.callList(list); }
    public static void glDeleteLists(int list, int range) {}

}
