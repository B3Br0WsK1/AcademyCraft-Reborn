package cn.academy.client.render.misc;
import cn.lambdalib2.render.legacy.GL20;
import static cn.lambdalib2.render.legacy.GL11.*;
import cn.academy.Resources;
import cn.lambdalib2.render.legacy.GLSLMesh;
import cn.lambdalib2.render.legacy.LegacyMeshUtils;
import cn.lambdalib2.render.legacy.ShaderSimple;
import cn.lambdalib2.renderhook.PlayerRenderHook;
import cn.lambdalib2.util.RenderUtils;
import cn.lambdalib2.renderhook.PlayerRenderHook;
import cn.lambdalib2.render.legacy.GLSLMesh;
import cn.lambdalib2.render.legacy.ShaderSimple;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.Identifier;

/*
import cn.lambdalib2.render.legacy.LegacyMeshUtils;*/


/**
 * @author WeAthFolD
 */
public class RailgunHandEffect extends PlayerRenderHook {

    static final double PER_FRAME = 40 / 1000.0;
    static final int COUNT = 40;

    Identifier[] textures;
    GLSLMesh mesh;
    
    public RailgunHandEffect() {
        textures = Resources.getEffectSeq("arc_burst", COUNT);
        mesh = new GLSLMesh();
        mesh = LegacyMeshUtils.createBillboard(mesh, -1, -1, 1, 1);
    }

    @Override
    public void renderHand(boolean firstPerson) {
        if(RenderUtils.isInShadowPass()) return;
        
        double dt = getElapsedTime();
        if(dt >= PER_FRAME * COUNT) {
            dispose();
            return;
        }
        
        int frame = (int) (dt / PER_FRAME);
        
        glEnable(GL_ALPHA_TEST);
        glAlphaFunc(GL_GREATER, 0.0f);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        ShaderSimple.instance().useProgram();
        glPushMatrix();
        Player player = Minecraft.getInstance().player;
        if(firstPerson) {
            double pitchRad = Math.toRadians(player.getXRot());
            double eyeHeight = player.getEyeHeight();
            glTranslated(0, Math.cos(pitchRad) * eyeHeight, Math.sin(pitchRad) * eyeHeight);
            glTranslated(.26, -.15, -.24);
            glScalef(.4f, .4f, 1f);
        } else {
            // Around the right hand (the origin is 0.8 above the feet + 0.15 to the right here); the original
            //  placed the 2x2 billboard 2.6 blocks up, floating over the head.
            glTranslated(0.2, 0.45, -0.45);
            glRotated(-player.getXRot(), 1, 0, 0);
            glScalef(.7f, .7f, 1f);
        }
        RenderUtils.loadTexture(textures[frame]);
        mesh.draw(ShaderSimple.instance());
        glPopMatrix();
        GL20.glUseProgram(0);
        glAlphaFunc(GL_GEQUAL, 0.1f);
        glEnable(GL_CULL_FACE);
    }
    
}