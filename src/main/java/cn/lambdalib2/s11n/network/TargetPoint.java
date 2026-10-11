package cn.lambdalib2.s11n.network;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * A point in a dimension with a range, used to select which players receive a message.
 */
public class TargetPoint {
    public final ResourceKey<Level> dimension;
    public final double x, y, z;
    public final double range;

    public TargetPoint(ResourceKey<Level> dimension, double x, double y, double z, double range) {
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.range = range;
    }
}
