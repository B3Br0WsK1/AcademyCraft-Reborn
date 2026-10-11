package cn.academy.ability.develop.action;
import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.datapart.AbilityData;
import cn.academy.ability.develop.IDeveloper;
import cn.academy.ability.develop.LearningHelper;
import cn.academy.item.ItemInductionFactor;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;



/**
 * @author WeAthFolD
 */
public class DevelopActionLevel implements IDevelopAction {

    @Override
    public int getStimulations(Player player) {
        return 5 * (AbilityData.get(player).getLevel() + 1);
    }

    @Override
    public boolean validate(Player player, IDeveloper developer) {
        return LearningHelper.canLevelUp(developer.getDeveloperType(), AbilityData.get(player));
    }

    @Override
    public void onLearned(Player player) {
        AbilityData aData = AbilityData.get(player);
        if(!aData.hasCategory()) {
            aData.setCategory(chooseCategory(player));
        } else {
            aData.setLevel(aData.getLevel() + 1);
        }
    }

    private Category chooseCategory(Player player) {
        Optional<ItemStack> inductedCategory = DevelopActionReset.getFactor(player);
        if (inductedCategory.isPresent()) {
            ItemStack factor = inductedCategory.get();
            int factorIdx = player.getInventory().getNonEquipmentItems().indexOf(factor);
            player.getInventory().setItem(factorIdx, ItemStack.EMPTY);
            return ItemInductionFactor.getCategory(factor);
        } else {
            CategoryManager man = CategoryManager.INSTANCE;
            return man.getCategory(RandUtils.nextInt(man.getCategoryCount()));
        }
    }

}