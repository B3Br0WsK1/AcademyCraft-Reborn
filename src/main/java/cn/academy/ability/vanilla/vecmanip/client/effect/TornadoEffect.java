package cn.academy.ability.vanilla.vecmanip.client.effect;

import cn.lambdalib2.util.GameTimer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.List;

import static cn.lambdalib2.util.RandUtils.RNG;
import static cn.lambdalib2.util.RandUtils.ranged;

/**
 * Ported from Scala. Rendered by {@link TornadoRenderer}.
 */
@Environment(EnvType.CLIENT)
public class TornadoEffect {

    public static final int divide = 40;

    public record Ring(double y, double width, double phase, double sizeScale) {}

    public final double ht, sz, density, dscale;

    private final List<Ring> rings = new ArrayList<>();
    private final double timeOffset = RNG.nextDouble() * 20;

    public double alpha = 1.0;

    public TornadoEffect(double ht, double sz) {
        this(ht, sz, 1.0, 1.0);
    }

    public TornadoEffect(double ht, double sz, double density, double dscale) {
        this.ht = ht;
        this.sz = sz;
        this.density = density;
        this.dscale = dscale;

        double accum = 0.0;
        double stdstep = ht / divide;
        while (accum < ht) {
            accum += stdstep * (1.0 + RNG.nextGaussian() * 0.2);

            if (RNG.nextDouble() < density) {
                rings.add(new Ring(accum, stdstep * ranged(1.8, 2.2), RNG.nextDouble() * 360, ranged(0.9, 1.2)));

                if (RNG.nextDouble() < 0.35) {
                    rings.add(new Ring(accum, stdstep * ranged(1.8, 2.2), RNG.nextDouble() * 360, ranged(1.2, 1.7)));
                }
            }
        }
    }

    public double time() {
        return GameTimer.getTime() * 4.0 - timeOffset;
    }

    public List<Ring> getRings() {
        return rings;
    }

}
