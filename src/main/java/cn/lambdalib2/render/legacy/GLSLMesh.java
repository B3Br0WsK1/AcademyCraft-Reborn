package cn.lambdalib2.render.legacy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * External mesh drawer using shader. In the emulation, the shader is ignored and the mesh is drawn with the current
 *  GL state.
 * @author WeAthFolD
 */
@Environment(EnvType.CLIENT)
public class GLSLMesh extends LegacyMesh {

    public GLSLMesh() {}

    public void draw(LegacyShaderProgram program) {
        draw(program == null ? 0 : program.getProgramID());
    }

    /**
     * Draw the mesh using the specified shader program.
     */
    public void draw(int programID) {
        if (triangles == null) return;
        LegacyGL.begin(GL11.GL_TRIANGLES);
        for(int j = 0; j < triangles.length; ++j) {
            int i = triangles[j];
            if(uvs != null) {
                LegacyGL.texCoord(uvs[i][0], uvs[i][1]);
            } else {
                LegacyGL.texCoord(0, 0);
            }
            LegacyGL.vertex(vertices[i][0], vertices[i][1], vertices[i][2]);
        }
        LegacyGL.end();
    }

    @Override
    public void draw(LegacyMaterial mat) {
        mat.onRenderStage(RenderStage.START);
        mat.onRenderStage(RenderStage.TRANSFORM);
        mat.onRenderStage(RenderStage.BEFORE_TESSELLATE);
        draw(0);
        mat.onRenderStage(RenderStage.END);
    }

    @Override
    public void redraw(LegacyMaterial mat) {
        draw(mat);
    }

}
