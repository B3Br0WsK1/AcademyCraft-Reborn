package cn.academy.client.render.misc;
import cn.lambdalib2.particle.ParticleTemplate;
import cn.academy.Resources;
import cn.lambdalib2.particle.Particle;
import cn.lambdalib2.particle.ParticleFactory;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.entityx.MotionHandler;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import java.util.Random;
import net.fabricmc.api.EnvType;



@Environment(EnvType.CLIENT)
public class IronSandParticles {
    
    Identifier[] textures = Resources.getEffectSeq("ironsand", 5);
    
    Random rand = new Random();
    ParticleFactory factory;
    
    final Player player;
    
    public IronSandParticles(Player _player) {
        player = _player;
        
        ParticleTemplate template = new ParticleTemplate();
        template.size = 0.6f;
        template.needRigidbody = false;
        
        factory = new ParticleFactory(template) {
            @Override
            public Particle next(Level world) {
                Particle ret = super.next(world);
                final long time = RandUtils.rangei(3000, 4000);
                
                ret.fadeAfter(RandUtils.rangei(40, 60), 20);
                ret.fadeInTime = 10;
                
                double theta = rand.nextDouble() * Math.PI * 2;
                double r = 1.7 + RandUtils.ranged(-0.1, 0.1);
                double dy = RandUtils.ranged(-1.6, -1.5);
                
                double dvR = RandUtils.ranged(-0.01, 0.01), dvY = RandUtils.ranged(0.01, 0.02);
                double spin = 3;
                
                ret.texture = textures[RandUtils.rangei(0, 5)];
                ret.setPosition(
                    player.getX() + r * Math.sin(theta), 
                    player.getY() + dy, 
                    player.getZ() + r * Math.cos(theta));
                
                ret.addMotionHandler(new MotionHandler<Particle>() {
                    
                    double rad = r;

                    @Override
                    public String getID() {
                        return "surround";
                    }

                    @Override
                    public void onStart() { onUpdate(); }

                    @Override
                    public void onUpdate() {
                        Particle target = getTarget();
                        double time = (target.getParticleLife() / 1000.0) % (Math.PI * 2);
                        target.setPos(player.getX() + rad * Math.sin(theta + spin * time), target.getY(), target.getZ());
                        target.setPos(target.getX(), target.getY() + dvY, target.getZ());
                        target.setPos(target.getX(), target.getY(), player.getZ() + rad * Math.cos(theta + spin * time));
                        rad += dvR;
                    }
                    
                });
                return ret;
            }
        };
    }
    
    public void tick() {
        int n = RandUtils.rangei(2, 3);
        for(int i = 0; i < n; ++i) {
            player.level().addFreshEntity(factory.next(player.level()));
        }
    }
}