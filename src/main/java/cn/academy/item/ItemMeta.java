package cn.academy.item;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/**
 * Replacement of 1.12 item metadata (sub types). The value is stored in the {@code academy:meta} data component,
 *  which item model definitions can select on.
 */
public final class ItemMeta {

    public static final DataComponentType<Integer> META = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE,
        Identifier.fromNamespaceAndPath("academy", "meta"),
        DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    private ItemMeta() {}

    /**
     * Forces class loading so that the component gets registered.
     */
    public static void init() {}

    public static ItemStack of(ItemLike item, int count, int meta) {
        ItemStack stack = new ItemStack(item, count);
        set(stack, meta);
        return stack;
    }

    public static ItemStack of(ItemLike item, int meta) {
        return of(item, 1, meta);
    }

    public static int get(ItemStack stack) {
        Integer v = stack.get(META);
        return v == null ? 0 : v;
    }

    public static void set(ItemStack stack, int meta) {
        if (meta == 0) {
            stack.remove(META);
        } else {
            stack.set(META, meta);
        }
    }

    public static boolean matches(ItemStack stack, Item item, int meta) {
        return stack.getItem() == item && get(stack) == meta;
    }
}
