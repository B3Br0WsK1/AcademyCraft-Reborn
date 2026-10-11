package cn.academy.entity;
import cn.academy.ACEntities;
import cn.academy.client.render.util.ACRenderingHelper;
import cn.academy.Resources;
import cn.academy.client.render.entity.ray.RendererRayComposite;
import cn.academy.client.render.particle.MdParticleFactory;
import cn.lambdalib2.particle.Particle;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.VecUtils;
import cn.lambdalib2.render.legacy.RenderManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;


/**
 * @author WeAthFolD
 */
@Environment(EnvType.CLIENT)
public class EntityMineRayLuck extends EntityRayBase {
    
    static final Identifier texture = Resources.getTexture("effects/md_particle_luck");
    
    public EntityMineRayLuck(Player _player) {
        super(ACEntities.MINE_RAY_LUCK, _player);

        this.blendInTime = 200;
        this.blendOutTime = 400;
        this.life = 233333;
        this.length = 15.0;
        updatePos();
    }
    
    @Override
    public void onUpdate() {
        super.onUpdate();
        Player player = getPlayer();
        updatePos();

        if(RandUtils.nextDouble() < 0.6) {
            Particle p = MdParticleFactory.INSTANCE.next(world,
//                    new Motion3D(this, true).move(RandUtils.ranged(0, 10)).getPosVec(),
                    VecUtils.lookingPos(player, RandUtils.ranged(0, 10)),
                    new Vec3(RandUtils.ranged(-.03, .03), RandUtils.ranged(-.03, .03), RandUtils.ranged(-.03, .03)));
            p.texture = texture;
//            world.spawnEntityInWorld(p);
            world.addFreshEntity(p);
        }
    }

    private void updatePos() {
        Player player = getPlayer();
        Vec3 end = VecUtils.lookingPos(player, 15); //new Motion3D(player, true).move(15).getPosVec();
        this.setFromTo(player.getX(), player.getY() + 1.55, player.getZ(), end.x, end.y, end.z);
    }
    public static class LuckRayRender extends RendererRayComposite {

        public LuckRayRender(RenderManager manager) {
            super(manager, "mdray_luck");
            this.cylinderIn.width = 0.04;
            this.cylinderIn.color.set(241, 229, 247, 230);
            
            this.cylinderOut.width = 0.05;
            this.cylinderOut.color.set(205, 166, 232, 50);
            
            this.glow.width = 0.45;
            this.glow.color.setAlpha(Colors.f2i(0.6f));
        }
        
    }
}