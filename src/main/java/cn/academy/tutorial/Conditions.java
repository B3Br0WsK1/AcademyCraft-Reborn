package cn.academy.tutorial;
import cn.lambdalib2.registry.StateEventCallback;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.event.SubscribeEvent;
import cn.lambdalib2.event.mc.PlayerEvents.ItemCraftedEvent;
import cn.lambdalib2.event.mc.PlayerEvents.ItemPickupEvent;
import cn.lambdalib2.event.mc.PlayerEvents.ItemSmeltedEvent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import cn.lambdalib2.event.EventHub;
import java.util.ArrayList;
import java.util.List;



/**
 * Factory of various kinds of conditions. Condition can be only created from this class.
 */
public class Conditions {

    private Conditions() {}

    private static final List<Condition> indexedConditions = new ArrayList<>();
    private static final Multimap<Item, ItemInfo>
        craftConds = ArrayListMultimap.create(),
        smeltConds = ArrayListMultimap.create(),
        pickupConds = ArrayListMultimap.create();

    public static Condition alwaysTrue() {
        return player -> true;
    }

    public static Condition itemCrafted(Item item) {
        return itemCrafted(item, -1);
    }

    public static Condition itemCrafted(Item item, int meta) {
        return createItemMapped(craftConds, item, meta);
    }

    public static Condition itemSmelted(Item item) {
        return itemSmelted(item, -1);
    }

    public static Condition itemSmelted(Item item, int meta) {
        return createItemMapped(smeltConds, item, meta);
    }

    public static Condition itemPickup(Item item) {
        return itemPickup(item, -1);
    }

    public static Condition itemPickup(Item item, int meta) {
        return createItemMapped(pickupConds, item, meta);
    }

    public static Condition itemObtained(Item item) {
        return itemCrafted(item).or(itemPickup(item)).or(itemSmelted(item));
    }

    public static Condition itemObtained(Item item, int meta) {
        return itemCrafted(item, meta).or(itemPickup(item, meta)).or(itemSmelted(item, meta));
    }

    public static Condition itemObtained(Block block) {
        return itemObtained(block.asItem());
    }

    public static Condition itemObtained(Block block, int meta) {
        return itemObtained(block.asItem(), meta);
    }

    private static IndexedCondition indexed() {
        int idx = indexedConditions.size();
        IndexedCondition ret = new IndexedCondition(idx);
        indexedConditions.add(ret);
        return ret;
    }

    private static Condition createItemMapped(Multimap<Item, ItemInfo> map, Item item, int meta) {
        IndexedCondition ret = indexed();
        map.put(item, new ItemInfo(ret, item, meta));

        return ret;
    }

    private static class IndexedCondition implements Condition {

        final int index;

        IndexedCondition(int idx) {
            index = idx;
        }

        @Override
        public boolean test(Player entityPlayer) {
            return TutorialData.get(entityPlayer).isCondActivate(index);
        }
    }

    private static class ItemInfo {

        public final IndexedCondition cond;
        public final Item item;
        public final int meta;

        public ItemInfo(IndexedCondition cond, Item item, int meta) {
            this.cond = cond;
            this.item = item;
            this.meta = meta;
        }

        public boolean metaSensitive() {
            return meta != -1;
        }

    }

    @StateEventCallback
    private static void _init(StateEvents.Init ev) {
        Conditions instance = new Conditions();
        EventHub.BUS.register(instance);
    }

    @SubscribeEvent
    public void onItemSmelt(ItemSmeltedEvent evt) {
        trigger(smeltConds, evt.smelting, evt.getEntityPlayer());
    }

    @SubscribeEvent
    public void onItemCraft(ItemCraftedEvent evt) {
        trigger(craftConds, evt.crafting, evt.getEntityPlayer());
    }

    @SubscribeEvent
    public void onItemPickup(ItemPickupEvent evt) {
        trigger(pickupConds, evt.getStack(), evt.getEntityPlayer());
    }

    private void trigger(Multimap<Item, ItemInfo> map, ItemStack stack, Player player) {
        if (!player.level().isClientSide()) {
            TutorialData tdata = TutorialData.get(player);
            map.get(stack.getItem())
                    .stream()
                    .filter(info -> !info.metaSensitive() || cn.academy.item.ItemMeta.get(stack) == info.meta)
                    .forEach(info -> {
                        tdata.setCondActivate(info.cond.index);
                    });
        }
    }

}