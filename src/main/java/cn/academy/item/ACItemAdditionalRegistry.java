package cn.academy.item;

import cn.academy.ACItems;
import cn.academy.misc.media.MediaManager;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import java.util.Set;

/**
 * Adds AcademyCraft items into vanilla chest loot.
 */
public class ACItemAdditionalRegistry {

    private static final Set<ResourceKey<LootTable>> DEFAULT_APPEARANCE = Set.of(
        BuiltInLootTables.ABANDONED_MINESHAFT,
        BuiltInLootTables.DESERT_PYRAMID,
        BuiltInLootTables.JUNGLE_TEMPLE,
        BuiltInLootTables.STRONGHOLD_LIBRARY,
        BuiltInLootTables.SIMPLE_DUNGEON);

    @StateEventCallback
    private static void init(StateEvents.Init evt)
    {
        LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
            if (!source.isBuiltin() || !DEFAULT_APPEARANCE.contains(key))
                return;

            // Induction factors (4 categories, total weight 10 against 90 empty)
            LootPool.Builder factors = LootPool.lootPool().setRolls(ContextIntProviders.exactly(4));
            for (int i = 0; i < 4; ++i) {
                factors.add(LootItem.lootTableItem(ACItems.induction_factor).setWeight(i == 0 ? 4 : 2)
                    .apply(SetComponentsFunction.setComponent(ItemMeta.META, i)));
            }
            factors.add(EmptyLootItem.emptyItem().setWeight(90));
            builder.withPool(factors);

            // Media discs
            int medias = MediaManager.internalMediaCount();
            if (medias > 0) {
                LootPool.Builder music = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1));
                for (int i = 0; i < medias; ++i) {
                    music.add(LootItem.lootTableItem(ACItems.media_item).setWeight(Math.max(1, 10 / medias))
                        .apply(SetComponentsFunction.setComponent(ItemMeta.META, i)));
                }
                music.add(EmptyLootItem.emptyItem().setWeight(90));
                builder.withPool(music);
            }
        });
    }

}
