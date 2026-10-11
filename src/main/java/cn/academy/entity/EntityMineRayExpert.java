package cn.academy.entity;
import cn.academy.ACEntities;
import cn.academy.client.render.util.ACRenderingHelper;
import cn.academy.client.render.entity.ray.RendererRayComposite;
import cn.academy.client.render.particle.MdParticleFactory;
import cn.lambdalib2.particle.Particle;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.VecUtils;
import cn.lambdalib2.render.legacy.RenderManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;


/**
 * @author WeAthFolD
 */
@Environment(EnvType.CLIENT)
public class EntityMineRayExpert extends EntityRayBase {

    public static ExpertRayRenderer renderer;
    
    public EntityMineRayExpert(Player _player) {
        super(ACEntities.MINE_RAY_EXPERT, _player);

        this.blendInTime = 200;
        this.blendOutTime = 400;
        this.life = 233333;
        this.length = 15.0;

        updatePos();
    }
    
    @Override
    public void onUpdate() {
        super.onUpdate();
        updatePos();

        Player player = getPlayer();
        if(RandUtils.nextDouble() < 0.6) {
            Particle p = MdParticleFactory.INSTANCE.next(world,
//                    new Motion3D(this, true).move(RandUtils.ranged(0, 10)).getPosVec(),
                    VecUtils.lookingPos(player, RandUtils.ranged(0, 10)),
                    new Vec3(RandUtils.ranged(-.03, .03), RandUtils.ranged(-.03, .03), RandUtils.ranged(-.03, .03)));
//            world.spawnEntityInWorld(p);
            world.addFreshEntity(p);
        }
    }

    private void updatePos() {
        Player player = getPlayer();
        Vec3 end = VecUtils.lookingPos(player, 15); //new Motion3D(player, true).move(15).getPosVec();
        this.setFromTo(player.getX(), player.getY() + 1.55, player.getZ(), end.x, end.y, end.z);
    }
    public static class ExpertRayRenderer extends RendererRayComposite {

        public ExpertRayRenderer(RenderManager manager) {
            super(manager, "mdray_expert");
            this.cylinderIn.width = 0.045;
            this.cylinderIn.color.set(216, 248, 216, 230);
            
            this.cylinderOut.width = 0.056;
            this.cylinderOut.color.set(106, 242, 106, 50);
            
            this.glow.width = 0.5;
            this.glow.color.setAlpha(Colors.f2i(0.7f));
        }
        
        @Override
        public void doRender(Entity ent, double x,
                double y, double z, float a, float b) {
            this.cylinderIn.width = 0.045;
            this.cylinderIn.color.set(216, 248, 216, 180);
            
            this.cylinderOut.width = 0.056;
            this.cylinderOut.color.set(106, 242, 106, 50);
            
            this.glow.color.setAlpha(Colors.f2i(0.5f));
            super.doRender(ent, x, y, z, a ,b);
        }
        
    }
}