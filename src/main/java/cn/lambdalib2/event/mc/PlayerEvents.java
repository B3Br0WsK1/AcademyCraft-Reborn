package cn.lambdalib2.event.mc;

import cn.lambdalib2.event.Event;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Player-related events. Counterpart of Forge's PlayerEvent family.
 */
public final class PlayerEvents {

    private PlayerEvents() {}

    public static class PlayerEvent extends Event {
        public final Player player;

        public PlayerEvent(Player player) {
            this.player = player;
        }

        public Player getEntityPlayer() {
            return player;
        }

        public Player getPlayer() {
            return player;
        }
    }

    /**
     * Fired when a player is recreated (respawn / dimension change) on server.
     */
    public static class Clone extends PlayerEvent {
        private final Player original;
        public final boolean wasDeath;

        public Clone(Player newPlayer, Player original, boolean wasDeath) {
            super(newPlayer);
            this.original = original;
            this.wasDeath = wasDeath;
        }

        public Player getOriginal() {
            return original;
        }

        public boolean isWasDeath() {
            return wasDeath;
        }
    }

    public static class PlayerLoggedInEvent extends PlayerEvent {
        public PlayerLoggedInEvent(Player player) {
            super(player);
        }
    }

    public static class PlayerLoggedOutEvent extends PlayerEvent {
        public PlayerLoggedOutEvent(Player player) {
            super(player);
        }
    }

    public static class ItemCraftedEvent extends PlayerEvent {
        public final ItemStack crafting;

        public ItemCraftedEvent(Player player, ItemStack crafting) {
            super(player);
            this.crafting = crafting;
        }

        public ItemStack getCrafting() {
            return crafting;
        }
    }

    public static class ItemSmeltedEvent extends PlayerEvent {
        public final ItemStack smelting;

        public ItemSmeltedEvent(Player player, ItemStack smelting) {
            super(player);
            this.smelting = smelting;
        }

        public ItemStack getSmelting() {
            return smelting;
        }
    }

    public static class ItemPickupEvent extends PlayerEvent {
        private final ItemEntity originalEntity;
        private final ItemStack stack;

        public ItemPickupEvent(Player player, ItemEntity entPickedUp, ItemStack stack) {
            super(player);
            this.originalEntity = entPickedUp;
            this.stack = stack;
        }

        public ItemStack getStack() {
            return stack;
        }

        public ItemEntity getOriginalEntity() {
            return originalEntity;
        }
    }

    public static class PlayerWakeUpEvent extends PlayerEvent {
        /**
         * Whether the player was woken up forcefully (e.g. the bed was broken or the player was hurt).
         */
        public final boolean forcefulWakeUp;
        /**
         * Whether the sleeping players list should be updated (false when all players are woken up in the morning).
         */
        public final boolean updateLevelList;

        public PlayerWakeUpEvent(Player player, boolean forcefulWakeUp, boolean updateLevelList) {
            super(player);
            this.forcefulWakeUp = forcefulWakeUp;
            this.updateLevelList = updateLevelList;
        }
    }
}
