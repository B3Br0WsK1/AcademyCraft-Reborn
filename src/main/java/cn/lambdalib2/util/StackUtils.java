package cn.lambdalib2.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.Random;
import java.util.function.Consumer;

public class StackUtils {

    public static void dropItems(Level world, BlockPos pos, Container inv) {
        Random rand = new Random();

        for (int i = 0; i < inv.getContainerSize(); ++i) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                float rx = rand.nextFloat() * 0.8F + 0.1F;
                float ry = rand.nextFloat() * 0.8F + 0.1F;
                float rz = rand.nextFloat() * 0.8F + 0.1F;

                ItemEntity entityItem = new ItemEntity(world,
                    pos.getX() + rx, pos.getY() + ry, pos.getZ() + rz, stack.copy());

                float factor = 0.05F;
                entityItem.setDeltaMovement(rand.nextGaussian() * factor,
                    rand.nextGaussian() * factor + 0.2F,
                    rand.nextGaussian() * factor);
                world.addFreshEntity(entityItem);
                stack.setCount(0);
            }
        }
    }

    /**
     * Return whether two stack's item instance and data components are equal.
     */
    public static boolean isStackDataEqual(ItemStack s1, ItemStack s2) {
        return ItemStack.isSameItemSameComponents(s1, s2);
    }

    /**
     * @return A copy of the stack's custom data tag (empty tag if absent). Modifying the result doesn't affect the
     *  stack; use {@link #updateTag(ItemStack, Consumer)} to modify.
     */
    public static CompoundTag loadTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    /**
     * Modifies the custom data tag of the stack in place.
     */
    public static void updateTag(ItemStack stack, Consumer<CompoundTag> modifier) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, modifier);
    }

    public static void setTag(ItemStack stack, CompoundTag tag) {
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }

    public static boolean hasTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && !data.isEmpty();
    }

}
