package cn.academy.client.render.entity;
import static cn.lambdalib2.render.legacy.GL11.*;
import cn.academy.Resources;
import cn.academy.entity.EntityDiamondShield;
import cn.lambdalib2.render.legacy.LegacyMesh;
import cn.lambdalib2.render.legacy.SimpleMaterial;
import cn.lambdalib2.util.RenderUtils;
import cn.lambdalib2.render.legacy.LegacyRender;
import cn.lambdalib2.render.legacy.RenderManager;
import net.minecraft.resources.Identifier;



/**
 * @author WeAthFolD
 */
public class RenderDiamondShield extends LegacyRender<EntityDiamondShield> {
    
    LegacyMesh mesh;
    SimpleMaterial material;
    
    public RenderDiamondShield(RenderManager manager) {
        super(manager);
        mesh = new LegacyMesh();
        mesh.setVertices(new double[][] {
                { -1, 0, 0 },
                { 0, -1, 0 },
                { 1,  0, 0 },
                { 0, 1, 0 },
                { 0, 0, 1 }
        });
        mesh.setUVs(new double[][] {
                { 0, 0 },
                { 1, 1 },
                { 0, 0 },
                { 1, 1 },
                { 0, 1 }
        });
        mesh.setTriangles(new int[] {
            0, 1, 4,
            1, 2, 4,
            2, 3, 4,
            3, 0, 4
        });
        
        material = new SimpleMaterial(Resources.getTexture("effects/diamond_shield"));
        material.ignoreLight = true;
    }

    @Override
    public void doRender(EntityDiamondShield entity, double x,
            double y, double z, float a, float b) {
        if(RenderUtils.isInShadowPass())
            return;
        
        if(!entity.firstUpdated())
            return;
        
        glDisable(GL_CULL_FACE);
        glDisable(GL_ALPHA_TEST);
        glDisable(GL_DEPTH_TEST);
        glPushMatrix();
        
        glTranslated(x, y, z);
        
        glRotatef(-entity.getYRot(), 0, 1, 0);
        glRotatef(entity.getXRot(), 1, 0, 0);
        float s = 1.5f;
        glScalef(s, s, s);
        
        mesh.draw(material);
        
        glPopMatrix();
        glEnable(GL_ALPHA_TEST);
        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
    }
    
    @Override
    protected Identifier getEntityTexture(EntityDiamondShield entity) {
        return null;
    }

}