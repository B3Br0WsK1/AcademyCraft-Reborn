package cn.lambdalib2.particle;

import cn.lambdalib2.particle.decorators.ParticleDecorator;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * @author WeAthFolD
 */
public class ParticleFactory extends ParticleFactoryBase {

    public final ParticleTemplate template;

    private List<ParticleDecorator> decorators = new ArrayList<>();

    double px, py, pz;
    double vx, vy, vz;

    public ParticleFactory(ParticleTemplate _template) {
        template = _template;
    }

    public void addDecorator(ParticleDecorator dec) {
        decorators.add(dec);
    }

    public void setPosition(double x, double y, double z) {
        px = x;
        py = y;
        pz = z;
    }

    public void setVelocity(double x, double y, double z) {
        vx = x;
        vy = y;
        vz = z;
    }

    /**
     * Generate a specific particle in the world with the current position and
     * velocity settings.
     */
    @Override
    public Particle next(Level world) {
        Particle p = this.queryParticle(world);
        p.fromTemplate(template);

        p.setPos(px, py, pz);
        p.setDeltaMovement(vx, vy, vz);

        for (ParticleDecorator pd : decorators)
            pd.decorate(p);

        return p;
    }

    /**
     * Generates a particle at the given world position with the given velocity.
     * Note that this will override the current position and velocity settings.
     */
    public Particle next(Level world, Vec3 position, Vec3 velocity) {
        px = position.x;
        py = position.y;
        pz = position.z;

        vx = velocity.x;
        vy = velocity.y;
        vz = velocity.z;

        return next(world);
    }

}
