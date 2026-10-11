package cn.academy.client.render.entity.ray;
import cn.lambdalib2.render.legacy.GL11;
import static cn.lambdalib2.render.legacy.GL11.*;
import cn.academy.entity.IRay;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.RenderUtils;
import cn.lambdalib2.util.VecUtils;
import cn.lambdalib2.util.ViewOptimize;
import net.minecraft.client.Minecraft;
import cn.lambdalib2.render.legacy.OpenGlHelper;
import cn.lambdalib2.render.legacy.Tessellator;
import cn.lambdalib2.render.legacy.LegacyRender;
import cn.lambdalib2.render.legacy.RenderManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;



/**
 * Renderer to draw glow texture
 * @author WeAthFolD
 */
public abstract class RendererRayBaseGlow<T extends IRay> extends LegacyRender {
    
    {
        this.shadowOpaque = 0;
    }

    public RendererRayBaseGlow(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, 
            float yaw, float partialTicks) {
        T ray = (T) entity;
        
        Minecraft mc = Minecraft.getInstance();
        
        glPushMatrix();
        
        doTransform(ray);
        
        Vec3 position = ray.getRayPosition();
        Vec3 relativePosition = VecUtils.subtract(position,
                new Vec3(x, y, y));
        glTranslated(x, y, z);
        
        //Calculate the most appropriate 'billboard-up' direction.
        //The ray viewing direction.
        Vec3 dir = VecUtils.toDirVector(entity, partialTicks);
        //Pick two far enough start and end point.
        Vec3 start = VecUtils.multiply(dir, ray.getStartFix()),
            end = VecUtils.add(start, VecUtils.multiply(dir, ray.getLength() - ray.getStartFix()));
        
        Vec3 upDir;
        boolean firstPerson = ViewOptimize.isFirstPerson(ray);
        if(firstPerson) {
            upDir = new Vec3(0, 1, -0.5);
        } else {
            //Get closest point for view judging.
            Vec3 pt = new Vec3(0, 0, 0);
            
            //The player viewing direction towards pt.
            Vec3 perpViewDir = VecUtils.add(pt, relativePosition);
            
            // cross product to get the 'up' vector
            upDir = VecUtils.crossProduct(perpViewDir, dir);
        }
        
        upDir = upDir.normalize();
        
        //DEBUG
//        GL11.glDisable(GL11.GL_TEXTURE_2D);
//        Tessellator t = Tessellator.instance;
//        
//        t.startDrawing(GL11.GL_LINES);
//        //VecUtils.tessellate(start);
//        //VecUtils.tessellate(end);
//        
//        VecUtils.tessellate(pt);
//        VecUtils.tessellate(VecUtils.add(pt, VecUtils.scalarMultiply(upDir, 5)));
//        t.draw();
//        
//        GL11.glEnable(GL11.GL_TEXTURE_2D);
        //DEBUG END
        
        if(ray.needsViewOptimize()) {
            yaw = MathUtils.lerpDegree(entity.yRotO, entity.getYRot(), partialTicks);
            Vec3 vec = ViewOptimize.getFixVector(ray);
            vec = vec.yRot((float) ((270 - yaw) / 180 * Math.PI));
            start = VecUtils.add(start, vec);
            
            // Don't fix end to get accurate pointing direction
            // end = VecUtils.add(end, vec);
        }
        
        doTransform(ray);
        
        //Now delegate to the render itself~
        draw(ray, start, end, upDir);
        
        glPopMatrix();
    }
    
    protected void doPostTransform(T ray) {}
    
    protected void doTransform(T ray) {}
    
    protected void drawBoard(Vec3 start, Vec3 end, Vec3 upDir, double width) {
        width /= 2;
        Vec3
            v1 = VecUtils.add(start, VecUtils.multiply(upDir, width)),
            v2 = VecUtils.add(start, VecUtils.multiply(upDir, -width)),
            v3 = VecUtils.add(end,      VecUtils.multiply(upDir, -width)),
            v4 = VecUtils.add(end,   VecUtils.multiply(upDir, width));
        
        Tessellator t = Tessellator.instance;
        
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
        glBegin(GL_QUADS);
        RenderUtils.addVertexLegacy(v1, 0, 1);
        RenderUtils.addVertexLegacy(v2, 0, 0);
        RenderUtils.addVertexLegacy(v3, 1, 0);
        RenderUtils.addVertexLegacy(v4, 1, 1);
        glEnd();
    }
    
    /**
     * Draw the ray at the origin. The ray's heading direction should be toward x+, 
     * and normal is always in z direction.
     * @param start The start point
     * @param end The end point
     * @param sideDir the suggested billboard-up direction. You can ignore this if not drawing a billboard.
     */
    protected abstract void draw(T ray, Vec3 start, Vec3 end, Vec3 sideDir);

    @Override
    protected Identifier getEntityTexture(Entity p_110775_1_) {
        return null;
    }

}