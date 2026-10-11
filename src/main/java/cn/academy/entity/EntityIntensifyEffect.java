package cn.academy.entity;
import cn.academy.ACEntities;
import cn.academy.client.render.util.SubArc;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.entityx.EntityCallback;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;


/**
 * @author WeAthFolD
 */
@Environment(EnvType.CLIENT)
public class EntityIntensifyEffect extends EntitySurroundArc {

    public EntityIntensifyEffect(Player player) {
        super(ACEntities.INTENSIFY_EFFECT, player, 1.3);
        
        this.setArcType(ArcType.THIN);
        initEvents();
    }
    
    private void initEvents() {
        genAtHt(2, 0);
        genAtHt(1.8, 1);
        genAtHt(1.5, 3);
        genAtHt(1, 4);
        genAtHt(0.5, 6);
        genAtHt(0, 7);
        genAtHt(-0.1, 8);
        
        this.life = 15;
    }
    
    // Disable the original generation
    @Override
    protected void doGenerate() {}
    
    private void genAtHt(double ht, int after) {
        this.executeAfter((EntityCallback<EntityIntensifyEffect>) target -> {
            //arcHandler.clear();
            int gen = RandUtils.rangei(3, 4);
            while(gen-- > 0) {
                double phi = RandUtils.ranged(0.5, 0.6);
                double theta = RandUtils.ranged(0, Math.PI * 2);
                SubArc arc = arcHandler.generateAt(
                    new Vec3(phi * Math.sin(theta), ht, phi * Math.cos(theta)));
                arc.life = 3;
            }
        }, after);
    }

}