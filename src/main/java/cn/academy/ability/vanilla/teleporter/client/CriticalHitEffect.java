package cn.academy.ability.vanilla.teleporter.client;
import cn.academy.client.render.particle.FormulaParticleFactory;
import cn.academy.ability.vanilla.teleporter.util.TPSkillHelper.TPCritHitEvent;
import cn.lambdalib2.registry.mc.RegEventHandler;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.VecUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import cn.lambdalib2.event.EventHub;
import cn.lambdalib2.event.SubscribeEvent;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;


/**
 * @author WeAthFolD
 */
@Environment(EnvType.CLIENT)
public class CriticalHitEffect {
    private CriticalHitEffect() {}

    @RegEventHandler
    private static CriticalHitEffect instance = new CriticalHitEffect();

    @SubscribeEvent
    public void onTPCritHit(TPCritHitEvent event) {
        Level world = event.player.level();
        Entity t = event.target;
        if (world.isClientSide()) {
            int count = RandUtils.rangei(5, 8);
            while (count-- > 0) {
                double angle = RandUtils.ranged(0, Math.PI * 2);
                double r = RandUtils.ranged(t.getBbWidth() * .5, t.getBbWidth() * .7);
                double h = RandUtils.ranged(0, 1) * event.target.getBbHeight();

                world.addFreshEntity(FormulaParticleFactory.instance.next(world,
                        new Vec3(t.getX() + r * Math.sin(angle), t.getY() + h, t.getZ() + r * Math.cos(angle)),
                        VecUtils.multiply(VecUtils.random(), 0.03)));
            }
        }
    }

}