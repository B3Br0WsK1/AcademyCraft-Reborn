package cn.academy.entity;
import cn.academy.ACEntities;
import cn.academy.client.render.entity.ray.RendererRayComposite;
import cn.academy.client.render.particle.MdParticleFactory;
import cn.lambdalib2.particle.Particle;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.VecUtils;
import cn.lambdalib2.render.legacy.RenderManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;


/**
 * @author WeAthFolD
 */
@Environment(EnvType.CLIENT)
public class EntityMDRay extends EntityRayBase {

    public EntityMDRay(Player spawner, double length) {
        super(ACEntities.MD_RAY, spawner);

        Vec3 start = VecUtils.add(spawner.getEyePosition(1F), spawner.getLookAngle()),
                end = VecUtils.add(spawner.position(), VecUtils.multiply(spawner.getLookAngle(),length));
        this.setFromTo(start, end);
        this.blendInTime = 200;
        this.blendOutTime = 700;
        this.life = 50;
        this.length = length;
    }
    
    @Override
    public void onUpdate() {
        super.onUpdate();
        if(RandUtils.nextDouble() < 0.8) {
            Particle p = MdParticleFactory.INSTANCE.next(world,
//                    new Motion3D(this, true).move(RandUtils.ranged(0, 10)).getPosVec(),
                    VecUtils.lookingPos(this, RandUtils.ranged(0, 10)),
                    new Vec3(RandUtils.ranged(-.03, .03), RandUtils.ranged(-.03, .03), RandUtils.ranged(-.03, .03)));
            world.addFreshEntity(p);
        }
    }
    public static class MDRayRender extends RendererRayComposite {

        public MDRayRender(RenderManager manager) {
            super(manager, "mdray");
            this.cylinderIn.width = 0.17;
            this.cylinderIn.color.set(216, 248, 216, 230);
            
            this.cylinderOut.width = 0.22;
            this.cylinderOut.color.set(106, 242, 106, 50);
            
            this.glow.width = 1.5;
            this.glow.color.setAlpha(Colors.f2i(0.8f));
        }
        
    }
}