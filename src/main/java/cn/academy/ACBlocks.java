package cn.academy;

import cn.academy.ability.develop.DeveloperType;
import cn.academy.block.block.*;
import cn.academy.block.block.BlockNode.NodeType;
import cn.academy.block.fluid.ACFluids;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.function.Function;

/**
 * All the blocks of AcademyCraft. Class loading registers them (done in PreInit).
 */
public class ACBlocks {

    public static final BlockAbilityInterferer ability_interferer = register("ability_interferer",
        BlockAbilityInterferer::new, machine(0f));
    public static final BlockCatEngine cat_engine = register("cat_engine",
        BlockCatEngine::new, machine(0f).noOcclusion());
    public static final BlockGenericOre constraint_metal = register("constraint_metal",
        BlockGenericOre::new, ore(4.0f));
    public static final BlockGenericOre crystal_ore = register("crystal_ore",
        BlockGenericOre::new, ore(3.0f));
    public static final BlockDeveloper dev_advanced = register("dev_advanced",
        p -> new BlockDeveloper(DeveloperType.ADVANCED, p), machine(4.0f).noOcclusion());
    public static final BlockDeveloper dev_normal = register("dev_normal",
        p -> new BlockDeveloper(DeveloperType.NORMAL, p), machine(4.0f).noOcclusion());
    public static final BlockImagFusor imag_fusor = register("imag_fusor",
        BlockImagFusor::new, machine(3.0f).lightLevel(s -> s.getValue(BlockImagFusor.FRAME) > 0 ? 6 : 0));
    public static final BlockImagPhase imag_phase = register("imag_phase",
        p -> new BlockImagPhase(ACFluids.IMAG_PHASE, p),
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).replaceable().noCollision()
            .strength(100.0F).pushReaction(PushReaction.POPPED).noLootTable().liquid()
            .lightLevel(s -> 8).sound(SoundType.EMPTY));
    public static final BlockGenericOre imagsil_ore = register("imagsil_ore",
        BlockGenericOre::new, ore(3.75f));
    public static final Block machine_frame = register("machine_frame",
        Block::new, machine(0f));
    public static final BlockMatrix matrix = register("matrix",
        BlockMatrix::new, machine(3.0f).noOcclusion().lightLevel(s -> 15));
    public static final BlockMetalFormer metal_former = register("metal_former",
        BlockMetalFormer::new, machine(3.0f));
    public static final BlockNode node_advanced = register("node_advanced",
        p -> new BlockNode(NodeType.ADVANCED, p), machine(2.5f));
    public static final BlockNode node_basic = register("node_basic",
        p -> new BlockNode(NodeType.BASIC, p), machine(2.5f));
    public static final BlockNode node_standard = register("node_standard",
        p -> new BlockNode(NodeType.STANDARD, p), machine(2.5f));
    public static final BlockPhaseGen phase_gen = register("phase_gen",
        BlockPhaseGen::new, machine(2.5f).noOcclusion());
    public static final BlockGenericOre reso_ore = register("reso_ore",
        BlockGenericOre::new, ore(3.0f));
    public static final BlockSolarGen solar_gen = register("solar_gen",
        BlockSolarGen::new, machine(1.5f).noOcclusion());
    public static final BlockWindGenBase windgen_base = register("windgen_base",
        BlockWindGenBase::new, machine(4.0f).noOcclusion());
    public static final BlockWindGenMain windgen_main = register("windgen_main",
        BlockWindGenMain::new, machine(4.0f).noOcclusion());
    public static final BlockWindGenPillar windgen_pillar = register("windgen_pillar",
        BlockWindGenPillar::new, machine(4.0f).noOcclusion());

    private static BlockBehaviour.Properties machine(float hardness) {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .strength(hardness, Math.max(hardness, 1.0f) * 5.0f / 3.0f)
            .requiresCorrectToolForDrops()
            .sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties ore(float hardness) {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .strength(hardness, 5.0f)
            .requiresCorrectToolForDrops()
            .sound(SoundType.STONE);
    }

    private static <T extends Block> T register(String name, Function<BlockBehaviour.Properties, T> factory,
                                                BlockBehaviour.Properties props) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(AcademyCraft.MODID, name));
        T block = factory.apply(props.setId(key).overrideDescription("tile.ac_" + name + ".name"));
        return Registry.register(BuiltInRegistries.BLOCK, key, block);
    }

    /**
     * Forces class loading, which registers all the blocks.
     */
    public static void init() {}

}
