package cn.academy.ability.develop;
import cn.academy.ACItems;
import cn.academy.energy.api.IFItemManager;
import cn.lambdalib2.datapart.DataPart;
import cn.lambdalib2.datapart.EntityData;
import cn.lambdalib2.datapart.RegDataPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;


/**
 * The Developer instance for portable developer attached one per player.
 * @author WeAthFolD
 */
@RegDataPart(Player.class)
public class PortableDevData extends DataPart<Player> implements IDeveloper {
    
    public static PortableDevData get(Player player) {
        return EntityData.get(player).getPart(PortableDevData.class);
    }

    private ItemStack stack() {
        ItemStack stack = getEntity().getMainHandItem();
        return stack != null && stack.getItem() == ACItems.developer_portable ? stack : null;
    }

    @Override
    public DeveloperType getDeveloperType() {
        return DeveloperType.PORTABLE;
    }

    @Override
    public boolean tryPullEnergy(double amount) {
        ItemStack stack = stack();
        if(stack == null)
            return false;
        return IFItemManager.instance.pull(stack, amount, true) == amount;
    }

    @Override
    public double getEnergy() {
        ItemStack stack = stack();
        return stack == null ? 0 : IFItemManager.instance.getEnergy(stack);
    }

    @Override
    public double getMaxEnergy() {
        ItemStack stack = stack();
        return stack == null ? 0 : IFItemManager.instance.getMaxEnergy(stack);
    }
}