package cn.academy.event;
import cn.academy.energy.api.block.IWirelessTile;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import cn.lambdalib2.event.Event;


/**
 * Base class of any wireless event.
 * All WirelessEvent should only be executed in SERVER side.
 * @author WeathFolD
 */
public class WirelessEvent extends Event {

    public final IWirelessTile tile;

    public WirelessEvent(IWirelessTile _tile) {
        tile = _tile;
    }

    public BlockEntity getTileEntity() {
        return (BlockEntity) tile;
    }

    public Level getWorld() {
        return getTileEntity().getLevel();
    }

}