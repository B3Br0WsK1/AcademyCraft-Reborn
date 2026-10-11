package cn.academy.item;

import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import com.google.common.base.Preconditions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;

/**
 * Induction factor. The category id is stored as the {@link ItemMeta} value.
 */
public class ItemInductionFactor extends Item implements ISubItemProvider {

    public static Category getCategory(ItemStack stack) {
        checkStack(stack);
        return CategoryManager.INSTANCE.getCategory(ItemMeta.get(stack));
    }

    public static void setCategory(ItemStack stack, Category cat) {
        checkStack(stack);
        ItemMeta.set(stack, cat.getCategoryID());
    }

    private static void checkStack(ItemStack stack) {
        Preconditions.checkArgument(stack.getItem() instanceof ItemInductionFactor);
    }

    public ItemInductionFactor(Properties props) {
        super(props);
    }

    public ItemStack create(Category cat) {
        ItemStack stack = new ItemStack(this);
        setCategory(stack, cat);
        return stack;
    }

    @Override
    public void addSubItems(Consumer<ItemStack> items) {
        for (Category c : CategoryManager.INSTANCE.getCategories()) {
            items.accept(create(c));
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> builder, TooltipFlag flag) {
        Category cat = getCategory(stack);
        if (cat != null) {
            builder.accept(Component.literal(cat.getDisplayName()));
        }
    }

}
