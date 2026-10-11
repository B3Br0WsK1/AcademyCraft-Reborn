package cn.academy.item;

import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/**
 * Items that show several variants in the creative tab (replacement of 1.12 getSubItems).
 */
public interface ISubItemProvider {

    void addSubItems(Consumer<ItemStack> output);

}
