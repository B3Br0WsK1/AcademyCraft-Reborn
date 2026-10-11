package cn.academy.event;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import cn.lambdalib2.event.Cancelable;
import cn.lambdalib2.event.Event;


/**
 * Fire this event to determine whether a block is permissible to be destroyed
 * depending on current world and player's permissions.
 * Cancelled if it shouldn't be.
 * This event is internally listened by BukkitAdapter.
 * @author EAirPeter
 */
@Cancelable
public final class BlockDestroyEvent extends Event {

    public final Level world;
    public final Player player;
    public final BlockPos pos;

    public BlockDestroyEvent(Level world_, Player player_,
                             BlockPos pos_)
    {
        world = world_;
        player = player_;
        pos = pos_;
    }

    public BlockDestroyEvent(Level world_, BlockPos pos) {
        this(world_, null, pos);
    }
    
    public BlockDestroyEvent(Player player_, BlockPos pos) {
        this(player_.level(), player_, pos);
    }

}