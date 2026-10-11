package cn.academy.crafting;

import cn.academy.ACItems;
import cn.academy.block.tileentity.TileMetalFormer.Mode;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

// TODO: Make recipes data driven
public class MFIFRecipes {

    @StateEventCallback
    static void init(StateEvents.RegistriesReady ev) {
        ImagFusorRecipes ifr = ImagFusorRecipes.INSTANCE;
        ifr.addRecipe(new ItemStack(ACItems.crystal_low), 3000, new ItemStack(ACItems.crystal_normal));
        ifr.addRecipe(new ItemStack(ACItems.crystal_normal), 8000, new ItemStack(ACItems.crystal_pure));

        MetalFormerRecipes mfr = MetalFormerRecipes.INSTANCE;
        mfr.add(new ItemStack(ACItems.imag_silicon_ingot), new ItemStack(ACItems.wafer, 2), Mode.INCISE);
        mfr.add(new ItemStack(ACItems.wafer), new ItemStack(ACItems.imag_silicon_piece, 4), Mode.INCISE);
        mfr.add(new ItemStack(ACItems.data_chip), new ItemStack(ACItems.calc_chip), Mode.ETCH);
        mfr.add(new ItemStack(Items.IRON_INGOT), new ItemStack(ACItems.reinforced_iron_plate), Mode.PLATE);
        mfr.add(new ItemStack(ACItems.constraint_ingot), new ItemStack(ACItems.constraint_plate), Mode.PLATE);
        mfr.add(new ItemStack(ACItems.item_imagsil_ore), new ItemStack(ACItems.imag_silicon_ingot, 4), Mode.REFINE);
        mfr.add(new ItemStack(ACItems.item_constraint_metal), new ItemStack(ACItems.constraint_ingot, 2), Mode.REFINE);
        mfr.add(new ItemStack(ACItems.item_reso_ore), new ItemStack(ACItems.reso_crystal, 3), Mode.REFINE);
        mfr.add(new ItemStack(ACItems.item_crystal_ore), new ItemStack(ACItems.crystal_low, 4), Mode.REFINE);

        // Vanilla ores (the 1.12 version used the ore dictionary)
        addRefine(new ItemStack(Items.GOLD_INGOT, 2), Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Items.RAW_GOLD);
        addRefine(new ItemStack(Items.IRON_INGOT, 2), Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, Items.RAW_IRON);
        addRefine(new ItemStack(Items.COPPER_INGOT, 2), Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE, Items.RAW_COPPER);
        addRefine(new ItemStack(Items.EMERALD, 2), Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE);
        addRefine(new ItemStack(Items.QUARTZ, 2), Blocks.NETHER_QUARTZ_ORE);
        addRefine(new ItemStack(Items.DIAMOND, 2), Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
        addRefine(new ItemStack(Blocks.REDSTONE_BLOCK), Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE);
        addRefine(new ItemStack(Items.LAPIS_LAZULI, 12), Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE);
        addRefine(new ItemStack(Items.COAL, 2), Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE);
    }

    private static void addRefine(ItemStack output, ItemLike... inputs) {
        for (ItemLike in : inputs) {
            MetalFormerRecipes.INSTANCE.add(new ItemStack(in), output, Mode.REFINE);
        }
    }

}
