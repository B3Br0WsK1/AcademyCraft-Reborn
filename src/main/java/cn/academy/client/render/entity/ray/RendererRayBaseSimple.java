package cn.academy.client.render.entity.ray;
import cn.lambdalib2.render.legacy.GL11;
import cn.academy.entity.IRay;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.VecUtils;
import cn.lambdalib2.util.ViewOptimize;
import cn.lambdalib2.render.legacy.LegacyRender;
import cn.lambdalib2.render.legacy.RenderManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;



/**
 * @author WeAthFolD
 *
 */
public abstract class RendererRayBaseSimple<T extends Entity> extends LegacyRender<T> {

    {
        this.shadowOpaque = 0;
    }

    protected RendererRayBaseSimple(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    public void doRender(T ent,
        double x, double y, double z, float yaw, float partialTicks) {
        IRay ray = (IRay) ent;
        
        GL11.glPushMatrix();
        
        double length = ray.getLength();
        double fix = ray.getStartFix();
        
        Vec3 vo;
        if(ray.needsViewOptimize())
            vo = ViewOptimize.getFixVector(ray);
        else
            vo = new Vec3(0, 0, 0);
        // Rotate fix vector to world coordinate
        yaw = MathUtils.lerpDegree(ent.yRotO, ent.getYRot(), partialTicks);
        vo = vo.yRot(MathUtils.toRadians(270 - yaw));

        Vec3 start = new Vec3(0, 0, 0),
            end = VecUtils.add(start, VecUtils.multiply(VecUtils.toDirVector(ent, partialTicks), length));
        start = VecUtils.add(start, vo);

        x += start.x;
        y += start.y;
        z += start.z;
        
        Vec3 delta = VecUtils.subtract(end, start);
        double dxzsq = delta.x * delta.x + delta.z * delta.z;
        double npitch = MathUtils.toDegrees(Math.atan2(delta.y, Math.sqrt(dxzsq)));
        double nyaw = MathUtils.toDegrees(Math.atan2(delta.x, delta.z));
        
        GL11.glTranslated(x, y, z);
        GL11.glRotated(-90 + nyaw, 0, 1, 0);
        GL11.glRotated(npitch, 0, 0, 1);
        GL11.glTranslated(fix, 0, 0);
        draw(ent, ray.getLength() - fix);
        
        GL11.glPopMatrix();
    }
    
    /**
     * LegacyRender the ray in x+ direction. The transformation is automatically applied.
     * Note that if you want view optimizing, you must do it yourself( We don't know where is your begin pos and where is end).
     */
    protected abstract void draw(T entity, double suggestedLength);

    @Override
    protected Identifier getEntityTexture(T p_110775_1_) {
        return null;
    }

}