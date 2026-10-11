package cn.academy.ability.develop.action;
import cn.academy.ACItems;
import cn.academy.ability.Category;
import cn.academy.datapart.AbilityData;
import cn.academy.ability.develop.DeveloperType;
import cn.academy.ability.develop.IDeveloper;
import cn.academy.event.ability.TransformCategoryEvent;
import cn.academy.item.ItemInductionFactor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import cn.lambdalib2.event.EventHub;
import java.util.Optional;



public class DevelopActionReset implements IDevelopAction {

    public static boolean canReset(Player player, IDeveloper developer) {
        AbilityData data = AbilityData.get(player);
        ItemStack equip = player.getMainHandItem();
        Optional<ItemStack> factor = getFactor(player);

        int level = data.getLevel();

        return level >= 3 &&
                developer.getDeveloperType() == DeveloperType.ADVANCED &&
                !equip.isEmpty() && equip.getItem() == ACItems.magnetic_coil &&
                factor.isPresent() && ItemInductionFactor.getCategory(factor.get()) != data.getCategory();
    }

    static Optional<ItemStack> getFactor(Player player) {
        Category playerCategory = AbilityData.get(player).getCategoryNullable();
        return player.getInventory().getNonEquipmentItems().parallelStream()
                .filter(stack -> (!stack.isEmpty()) && stack.getItem() instanceof ItemInductionFactor)
                .filter(stack -> ItemInductionFactor.getCategory(stack) != playerCategory)
                .findAny();
    }

    @Override
    public int getStimulations(Player player) {
        AbilityData data = AbilityData.get(player);
        return data.getLevel() * 10;
    }

    @Override
    public boolean validate(Player player, IDeveloper developer) {
        return canReset(player, developer);
    }

    @Override
    public void onLearned(Player player) {
        AbilityData data = AbilityData.get(player);

        ItemStack factor = getFactor(player).get();

        Category newCat = ItemInductionFactor.getCategory(factor);

        int prevLevel = data.getLevel() - 1;
        if(!EventHub.BUS.post(new TransformCategoryEvent(player, newCat, prevLevel)))
        {
            data.setCategory(newCat);
            data.setLevel(prevLevel);

            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

            int factorIdx = player.getInventory().getNonEquipmentItems().indexOf(factor);
            player.getInventory().getNonEquipmentItems().set(factorIdx, ItemStack.EMPTY);
        }
    }
}