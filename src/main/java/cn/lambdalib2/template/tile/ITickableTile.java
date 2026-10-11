package cn.lambdalib2.template.tile;

/**
 * A tile entity that gets {@link #update()} called every tick on both sides (when its block uses the ticker of
 *  {@link TileEntityBase}-based blocks). Counterpart of 1.12 ITickable.
 */
public interface ITickableTile {
    void update();
}
