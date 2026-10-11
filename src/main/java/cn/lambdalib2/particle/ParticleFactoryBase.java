package cn.lambdalib2.particle;

import cn.lambdalib2.LLEntities;
import net.minecraft.world.level.Level;

/**
 * @author WeAthFolD
 */
public abstract class ParticleFactoryBase {

    public abstract Particle next(Level world);

    /**
     * Creates a new particle entity in the given world. (The 1.12 version pooled the particles; entities can't be
     *  reused in 26.x.)
     */
    protected final Particle queryParticle(Level world) {
        return new Particle(LLEntities.PARTICLE, world);
    }

}
