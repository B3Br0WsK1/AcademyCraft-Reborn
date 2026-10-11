package cn.academy;

import cn.academy.item.*;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * All the items of AcademyCraft. Class loading registers them (done in PreInit, after blocks).
 */
public class ACItems {

    private static final List<Item> tabOrder = new ArrayList<>();

    public static final ItemApp app_freq_transmitter = register("app_freq_transmitter",
        p -> new ItemApp("freq_transmitter", p), new Item.Properties(), "item.ac_apps.name");
    public static final ItemApp app_media_player = register("app_media_player",
        p -> new ItemApp("media_player", p), new Item.Properties(), "item.ac_apps.name");
    public static final ItemApp app_skill_tree = register("app_skill_tree",
        p -> new ItemApp("skill_tree", p), new Item.Properties(), "item.ac_apps.name");
    public static final Item brain_component = simple("brain_component");
    public static final Item calc_chip = simple("calc_chip");
    public static final ItemCoin coin = register("coin", ItemCoin::new, new Item.Properties());
    public static final Item constraint_ingot = simple("constraint_ingot");
    public static final Item constraint_plate = simple("constraint_plate");
    public static final Item crystal_low = simple("crystal_low");
    public static final Item crystal_normal = simple("crystal_normal");
    public static final Item crystal_pure = simple("crystal_pure");
    public static final Item data_chip = simple("data_chip");
    public static final ItemDeveloper developer_portable = register("developer_portable",
        ItemDeveloper::new, new Item.Properties().stacksTo(1).durability(13));
    public static final Item energy_convert_component = simple("energy_convert_component");
    public static final ItemEnergyBase energy_unit = register("energy_unit",
        p -> new ItemEnergyBase(10000, 20, p), new Item.Properties().stacksTo(1).durability(13));
    public static final Item imag_silicon_ingot = simple("imag_silicon_ingot");
    public static final Item imag_silicon_piece = simple("imag_silicon_piece");
    public static final ItemInductionFactor induction_factor = register("induction_factor",
        ItemInductionFactor::new, new Item.Properties().stacksTo(1));
    public static final Item info_component = simple("info_component");
    public static final Item logo = simple("logo");
    public static final ItemMagHook mag_hook = register("mag_hook", ItemMagHook::new, new Item.Properties());
    public static final ItemMagneticCoil magnetic_coil = register("magnetic_coil", ItemMagneticCoil::new, new Item.Properties());
    public static final ItemMatrixCore mat_core_0 = register("mat_core_0", p -> new ItemMatrixCore(0, p), new Item.Properties());
    public static final ItemMatrixCore mat_core_1 = register("mat_core_1", p -> new ItemMatrixCore(1, p), new Item.Properties());
    public static final ItemMatrixCore mat_core_2 = register("mat_core_2", p -> new ItemMatrixCore(2, p), new Item.Properties());
    public static final ItemMatterUnit matter_unit = register("matter_unit", ItemMatterUnit::new, new Item.Properties().stacksTo(16));
    public static final MediaItem media_item = register("media_item", MediaItem::new, new Item.Properties().stacksTo(1));
    public static final Item needle = simple("needle");
    public static final Item reinforced_iron_plate = simple("reinforced_iron_plate");
    public static final Item reso_crystal = simple("reso_crystal");
    public static final Item resonance_component = simple("resonance_component");
    public static final ItemSilbarn silbarn = register("silbarn", ItemSilbarn::new, new Item.Properties());
    public static final ItemTerminalInstaller terminal_installer = register("terminal_installer",
        ItemTerminalInstaller::new, new Item.Properties().stacksTo(1));
    public static final ItemTutorial tutorial = register("tutorial", ItemTutorial::new, new Item.Properties());
    public static final Item wafer = simple("wafer");
    public static final Item windgen_fan = register("windgen_fan", Item::new, new Item.Properties().stacksTo(1).durability(100));

    // Block items
    public static final BlockItem item_ability_interferer = blockItem(ACBlocks.ability_interferer, "ability_interferer");
    public static final BlockItem item_cat_engine = blockItem(ACBlocks.cat_engine, "cat_engine");
    public static final BlockItem item_constraint_metal = blockItem(ACBlocks.constraint_metal, "constraint_metal");
    public static final BlockItem item_crystal_ore = blockItem(ACBlocks.crystal_ore, "crystal_ore");
    public static final BlockItem item_dev_advanced = blockItem(ACBlocks.dev_advanced, "dev_advanced");
    public static final BlockItem item_dev_normal = blockItem(ACBlocks.dev_normal, "dev_normal");
    public static final BlockItem item_imag_fusor = blockItem(ACBlocks.imag_fusor, "imag_fusor");
    public static final BlockItem item_imagsil_ore = blockItem(ACBlocks.imagsil_ore, "imagsil_ore");
    public static final BlockItem item_machine_frame = blockItem(ACBlocks.machine_frame, "machine_frame");
    public static final BlockItem item_matrix = blockItem(ACBlocks.matrix, "matrix");
    public static final BlockItem item_metal_former = blockItem(ACBlocks.metal_former, "metal_former");
    public static final BlockItem item_node_advanced = blockItem(ACBlocks.node_advanced, "node_advanced");
    public static final BlockItem item_node_basic = blockItem(ACBlocks.node_basic, "node_basic");
    public static final BlockItem item_node_standard = blockItem(ACBlocks.node_standard, "node_standard");
    public static final BlockItem item_phase_gen = blockItem(ACBlocks.phase_gen, "phase_gen");
    public static final BlockItem item_reso_ore = blockItem(ACBlocks.reso_ore, "reso_ore");
    public static final BlockItem item_solar_gen = blockItem(ACBlocks.solar_gen, "solar_gen");
    public static final BlockItem item_windgen_base = blockItem(ACBlocks.windgen_base, "windgen_base");
    public static final BlockItem item_windgen_main = blockItem(ACBlocks.windgen_main, "windgen_main");
    public static final BlockItem item_windgen_pillar = blockItem(ACBlocks.windgen_pillar, "windgen_pillar");

    public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
        Identifier.fromNamespaceAndPath(AcademyCraft.MODID, "main"));

    public static final CreativeModeTab cct = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY,
        FabricCreativeModeTab.builder()
            .title(Component.translatable("itemGroup.AcademyCraft"))
            .icon(() -> new ItemStack(logo))
            .displayItems((params, output) -> {
                for (Item item : tabOrder) {
                    if (item instanceof ISubItemProvider sub) {
                        sub.addSubItems(output::accept);
                    } else {
                        output.accept(item);
                    }
                }
            })
            .build());

    private static Item simple(String name) {
        return register(name, Item::new, new Item.Properties());
    }

    private static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties props) {
        return register(name, factory, props, "item.ac_" + name + ".name");
    }

    private static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties props,
                                               String descriptionId) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(AcademyCraft.MODID, name));
        T item = factory.apply(props.setId(key).overrideDescription(descriptionId));
        Registry.register(BuiltInRegistries.ITEM, key, item);
        if (item != logo) {
            tabOrder.add(item);
        }
        return item;
    }

    private static BlockItem blockItem(Block block, String name) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(AcademyCraft.MODID, name));
        BlockItem item = new BlockItem(block, new Item.Properties().setId(key).overrideDescription("tile.ac_" + name + ".name"));
        item.registerBlocks(Item.BY_BLOCK, item);
        Registry.register(BuiltInRegistries.ITEM, key, item);
        tabOrder.add(item);
        return item;
    }

    /**
     * Forces class loading, which registers all the items.
     */
    public static void init() {}

}
