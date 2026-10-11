package cn.academy.entity;
import cn.academy.ACEntities;
import cn.academy.client.render.entity.ray.RendererRayComposite;
import cn.academy.client.sound.ACSounds;
import cn.academy.client.render.particle.MdParticleFactory;
import cn.lambdalib2.particle.Particle;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.VecUtils;
import cn.lambdalib2.render.legacy.RenderManager;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;


/**
 * @author WeAthFolD
 */
@Environment(EnvType.CLIENT)
public class EntityMdRaySmall extends EntityRayBase {

    public EntityMdRaySmall(Level world) {
        super(ACEntities.MD_RAY_SMALL, world);
        this.blendInTime = 200;
        this.blendOutTime = 400;
        this.life = 14;
        this.length = 15.0;
    }
    
    @Override
    protected void onFirstUpdate() {
        super.onFirstUpdate();
        ACSounds.playClient(world,getX(), getY(), getZ(), "md.ray_small",SoundSource.AMBIENT, 0.8f,1.0f);
    }
    
    @Override
    public void onUpdate() {
        super.onUpdate();
        Particle p = MdParticleFactory.INSTANCE.next(world,
//            new Motion3D(this, true).move(RandUtils.ranged(0, 10)).getPosVec(),
                VecUtils.lookingPos(this, RandUtils.ranged(0, 10)),
            new Vec3(RandUtils.ranged(-.015, .015), RandUtils.ranged(-.015, .015), RandUtils.ranged(-.015, .015)));
        world.addFreshEntity(p);
    }
    
    @Override
    public double getWidth() {
        long dt = getDeltaTime();
        int blendTime = 500;

        if(dt > this.life * 50 - blendTime) {
            double timeFactor = MathUtils.clampd(0, 1, (double) (dt - (this.life * 50 - blendTime)) / blendTime);
            return 1 - timeFactor;
        }
        
        return 1.0;
    }
    public static class SmallMdRayRender extends RendererRayComposite {

        public SmallMdRayRender(RenderManager manager) {
            super(manager, "mdray_small");
            this.cylinderIn.width = 0.03;
            this.cylinderIn.color.set(216, 248, 216, 230);
            
            this.cylinderOut.width = 0.045;
            this.cylinderOut.color.set(106, 242, 106, 50);
            
            this.glow.width = 0.3;
            this.glow.color.setAlpha(Colors.f2i(0.5f));
        }
        
    }

}