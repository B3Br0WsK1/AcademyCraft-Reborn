package cn.academy.event.ability;
import cn.academy.ability.Category;
import net.minecraft.world.entity.player.Player;


public class TransformCategoryEvent extends AbilityEvent
{
    public Category category;
    public int level;

    /**
     * Fire when player try to transform category by using ACItems.magnetic_coil.
     * This event is cancelable. If cancel, transforming progress will be aborted.
     * Command aim and aimp WILL NOT fire it. See {@see CategoryChangeEvent}
     *
     * @param _player who fire this event.
     * @param cat which player will convert to
     * @param level which player's level after transform.
     */
    public TransformCategoryEvent(Player _player, Category cat, int level)
    {
        super(_player);
        this.category = cat;
        this.level = level;
    }
}
