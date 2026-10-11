/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of LambdaLib modding library.
* https://github.com/LambdaInnovation/LambdaLib
* Licensed under MIT, see project root for more information.
*/
package cn.lambdalib2.util;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class PlayerUtils {

    /**
     * Try to merge an itemStack into the player inventory. The merging uses strict equality (same item and same
     *  components).
     * @return The stack size that is not merged into the inventory.
     */
    public static int mergeStackable(Inventory inv, ItemStack stack) {
        for(int i = 0; i < Inventory.INVENTORY_SIZE && stack.getCount() > 0; ++i) {
            ItemStack is = inv.getItem(i);
            if(!is.isEmpty() && StackUtils.isStackDataEqual(stack, is)) {
                is.grow(stack.getCount());
                int left = Math.max(0, is.getCount() - is.getMaxStackSize());
                stack.setCount(left);
                is.shrink(left);
            }
        }
        if(stack.getCount() > 0) {
            int id = inv.getFreeSlot();
            if(id == -1) {
                return stack.getCount();
            }
            inv.setItem(id, stack.copy());
            return 0;
        }
        return 0;
    }

    /**
     * Try to find the index of a item in player's inventory. if fail, return -1.
     */
    public static int getSlotByStack(ItemStack item, Player player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); ++i) {
            if (inv.getItem(i) == item) return i;
        }
        return inv.findSlotMatchingItem(item);
    }

    /**
     * Sends a translatable chat message to the player.
     */
    public static void sendChat(Player player, String message, Object... pars) {
        player.sendSystemMessage(Component.translatable(message, pars));
    }


    /**
     * Drops the item stack at the player's position (server side only). Replacement of 1.12 player.dropItem.
     */
    public static void dropItem(net.minecraft.world.entity.player.Player player, ItemStack stack) {
        if (stack.isEmpty() || player.level().isClientSide()) return;
        net.minecraft.world.entity.item.ItemEntity e = new net.minecraft.world.entity.item.ItemEntity(
            player.level(), player.getX(), player.getY() + 0.5, player.getZ(), stack);
        e.setPickUpDelay(10);
        player.level().addFreshEntity(e);
    }

}
