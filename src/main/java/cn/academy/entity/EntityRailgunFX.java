package cn.academy.entity;
import cn.academy.ACEntities;
import cn.lambdalib2.render.legacy.GL11;
import cn.academy.client.render.entity.ray.RendererRayComposite;
import cn.academy.client.sound.ACSounds;
import cn.academy.client.render.util.ArcFactory;
import cn.academy.client.render.util.ArcFactory.Arc;
import cn.academy.client.render.util.SubArcHandler;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.VecUtils;
import cn.lambdalib2.util.ViewOptimize;
import cn.lambdalib2.render.legacy.RenderManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;


/**
 * @author WeAthFolD
 *
 */
@Environment(EnvType.CLIENT)
public class EntityRailgunFX extends EntityRayBase {
    
    static final int ARC_SIZE = 15;

    static Arc[] templates;
    static {
        ArcFactory factory = new ArcFactory();
        factory.widthShrink = 0.9;
        factory.maxOffset = 0.8;
        factory.passes = 3;
        factory.width = 0.3;
        factory.branchFactor = 0.7;
        
        templates = new Arc[ARC_SIZE];
        for(int i = 0; i < ARC_SIZE; ++i) {
            templates[i] = factory.generate(RandUtils.ranged(2, 3));
        }
    }
    
    SubArcHandler arcHandler = new SubArcHandler(templates);
    
    public EntityRailgunFX(Player player, double length) {
        super(ACEntities.RAILGUN_FX, player);
        setPos(player.getX(), getY(), getZ());
        setPos(getX(), player.getY() + player.getEyeHeight(), getZ());
        setPos(getX(), getY(), player.getZ());

        this.setYRot(player.getYRot());
        this.setXRot(player.getXRot());
        
        this.life = 50;
        this.blendInTime = 150;
        this.widthShrinkTime = 800;
        this.widthWiggleRadius = 0.3;
        this.maxWiggleSpeed = 0.8;
        this.blendOutTime = 1000;
        this.length = length;
        //Build the arc list
        {
            double cur = 1.0;
            double len = this.length;
            
            while(cur <= len) {
                float theta = RandUtils.rangef(0, MathUtils.PI_F * 2);
                double r = RandUtils.ranged(0.1, 0.25);
                Vec3 vec = new Vec3(cur, r * Mth.sin(theta), r * Mth.cos(theta));
                VecUtils.rotateAroundZ(vec, getXRot() * MathUtils.PI_F / 180);
                vec.yRot((getYRot()) * MathUtils.PI_F / 180);
                arcHandler.generateAt(vec);
                
                cur += RandUtils.ranged(1, 2);
            }
        }
    }
    
    @Override
    protected void onFirstUpdate() {
        super.onFirstUpdate();
    }
    
    @Override
    public void onUpdate() {
        super.onUpdate();
        
        if(tickCount == 30)
            arcHandler.clear();
        
        arcHandler.tick();
    }
    public static class RailgunRender extends RendererRayComposite {
        
        Arc[] arcs;

        public RailgunRender(RenderManager manager) {
            super(manager, "railgun");
            glow.startFix = -0.3;
            glow.endFix = 0.3;
            glow.width = 1.1;
            
            cylinderIn.color.set(241, 240, 222, 200);
            cylinderIn.width = 0.09;
            
            cylinderOut.color.set(236, 170, 93, 60);
            cylinderOut.width = 0.13;
            
            ArcFactory factory = new ArcFactory();
            factory.widthShrink = 0.9;
            factory.maxOffset = 0.8;
            factory.passes = 3;
            factory.width = 0.3;
            factory.branchFactor = 0.7;
            
            arcs = new Arc[ARC_SIZE];
            for(int i = 0; i < ARC_SIZE; ++i) {
                arcs[i] = factory.generate(RandUtils.ranged(2, 3));
            }
        }
        
        @Override
        public void doRender(Entity ent, double x,
                double y, double z, float a, float b) {
            GL11.glPushMatrix();
            GL11.glTranslated(x, y, z);
            ViewOptimize.fix((ViewOptimize.IAssociatePlayer) ent);
            
            EntityRailgunFX railgun = (EntityRailgunFX) ent;
            
            railgun.arcHandler.drawAll();
            
            GL11.glPopMatrix();
            
            super.doRender(ent, x, y, z, a, b);
        }
        
    }

}