package cn.academy.event;
import cn.academy.item.ItemMatterUnit.MatterMaterial;
import cn.lambdalib2.event.Event;
import net.minecraft.world.entity.player.Player;


/**
 * @author WeAthFolD
 */
public class MatterUnitHarvestEvent extends Event {
    
    public final Player player;
    public final MatterMaterial mat;
    
    public MatterUnitHarvestEvent(Player _player, MatterMaterial _mat) {
        player = _player;
        mat = _mat;
    }
    
}