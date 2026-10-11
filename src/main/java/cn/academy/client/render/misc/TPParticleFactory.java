package cn.academy.client.render.misc;
import cn.lambdalib2.particle.ParticleTemplate;
import cn.academy.Resources;
import cn.lambdalib2.particle.Particle;
import cn.lambdalib2.particle.ParticleFactory;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.world.level.Level;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;


/**
 * @author WeAthFolD
 *
 */
public class TPParticleFactory extends ParticleFactory
{

    public static TPParticleFactory instance;

    static ParticleTemplate template;

    @Environment(EnvType.CLIENT)
    @StateEventCallback(priority = -100)
    private static void postInit(StateEvents.PostInit ev) {
        template = new ParticleTemplate();
        template.texture = Resources.getTexture("effects/tp_particle");
        template.size = 0.1f;
        template.hasLight = false;
        template.color.set(255, 255, 255, 255);

        instance = new TPParticleFactory();
    }

    public TPParticleFactory() {
        super(template);
    }

    @Override
    public Particle next(Level world) {
        Particle ret = super.next(world);
        ret.size = RandUtils.rangef(0.1f, 0.2f);
        ret.color.setAlpha(RandUtils.rangei(153, 204));
        ret.fadeAfter(20, 20);
        return ret;
    }

}