package cn.academy.entity;
import cn.academy.ACEntities;
import cn.academy.client.sound.ACSounds;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.render.legacy.RenderManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;


/**
 * This class uses some little hacks. By rendering all the barrage rays within a 
 * single render we avoid a fair amount of perfomance overheads.
 * @author WeAthFolD
 */
@Environment(EnvType.CLIENT)
public class EntityMdRayBarrage extends EntityRayBase {

    private SubRay[] subrays;

    public EntityMdRayBarrage(Level world, double x, double y, double z, float yaw, float pitch) {
        super(ACEntities.MD_RAY_BARRAGE, world);
        
        setPosition(x, y, z);
        setYRot(yaw);
        setXRot(pitch);
        
        this.life = 50;
        
        // Init the subrays
        final float range = RandUtils.rangef(50, 60);
        int max = RandUtils.rangei(25, 30);
        
        subrays = new SubRay[max];
        for(int i = 0; i < max; ++i)
            subrays[i] = new SubRay(range);
    }
    
    @Override
    protected void onFirstUpdate() {
        super.onFirstUpdate();
        ACSounds.playClient(world,getX(), getY(), getZ(), "md.ray_small",SoundSource.AMBIENT, 0.5f,1.0f);
    }
    
    @Override
    public boolean needsViewOptimize() {
        return false;
    }
    public static class BarrageRenderer extends EntityMdRaySmall.SmallMdRayRender {

        public BarrageRenderer(RenderManager manager) {
            super(manager);
        }

        @Override
        public void doRender(Entity ent, double x,
                double y, double z, float a, float b) {
            EntityMdRayBarrage ray = (EntityMdRayBarrage) ent;
            ray.onRenderTick();
            
            float rYaw = ent.getYRot(), rPitch = ent.getXRot();
            
            for(SubRay sr : ray.subrays) {
                ent.setYRot(rYaw + sr.yawOffset);
                ent.setXRot(rPitch + sr.pitchOffset);
                this.plainDoRender(ent, x, y, z, a, b);
            }
            
            ent.setYRot(rYaw);
            ent.setXRot(rPitch);
        }
        
    }
    
    private class SubRay {
        
        float yawOffset;
        float pitchOffset;
        
        public SubRay(float max) {
            yawOffset = RandUtils.rangef(-max, max);
            pitchOffset = RandUtils.rangef(-max / 2, max / 2);    
        }
        
    }

}