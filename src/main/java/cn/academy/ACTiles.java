package cn.academy;

import cn.academy.block.tileentity.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

/**
 * Block entity types of AcademyCraft.
 */
public class ACTiles {

    public static final BlockEntityType<TileAbilityInterferer> ABILITY_INTERFERER =
        register("ability_interferer", TileAbilityInterferer::new, ACBlocks.ability_interferer);
    public static final BlockEntityType<TileCatEngine> CAT_ENGINE =
        register("cat_engine", TileCatEngine::new, ACBlocks.cat_engine);
    public static final BlockEntityType<TileDeveloper.Normal> DEVELOPER_NORMAL =
        register("developer_normal", TileDeveloper.Normal::new, ACBlocks.dev_normal);
    public static final BlockEntityType<TileDeveloper.Advanced> DEVELOPER_ADVANCED =
        register("developer_advanced", TileDeveloper.Advanced::new, ACBlocks.dev_advanced);
    public static final BlockEntityType<TileImagPhase> IMAG_PHASE =
        register("imag_phase", TileImagPhase::new, ACBlocks.imag_phase);
    public static final BlockEntityType<TileImagFusor> IMAG_FUSOR =
        register("imag_fusor", TileImagFusor::new, ACBlocks.imag_fusor);
    public static final BlockEntityType<TileMatrix> MATRIX =
        register("matrix", TileMatrix::new, ACBlocks.matrix);
    public static final BlockEntityType<TileMetalFormer> METAL_FORMER =
        register("metal_former", TileMetalFormer::new, ACBlocks.metal_former);
    public static final BlockEntityType<TileNode> NODE =
        register("node", TileNode::new, ACBlocks.node_basic, ACBlocks.node_standard, ACBlocks.node_advanced);
    public static final BlockEntityType<TilePhaseGen> PHASE_GEN =
        register("phase_gen", TilePhaseGen::new, ACBlocks.phase_gen);
    public static final BlockEntityType<TileSolarGen> SOLAR_GEN =
        register("solar_gen", TileSolarGen::new, ACBlocks.solar_gen);
    public static final BlockEntityType<TileWindGenBase> WINDGEN_BASE =
        register("windgen_base", TileWindGenBase::new, ACBlocks.windgen_base);
    public static final BlockEntityType<TileWindGenMain> WINDGEN_MAIN =
        register("windgen_main", TileWindGenMain::new, ACBlocks.windgen_main);
    public static final BlockEntityType<TileWindGenPillar> WINDGEN_PILLAR =
        register("windgen_pillar", TileWindGenPillar::new, ACBlocks.windgen_pillar);

    private static <T extends BlockEntity> BlockEntityType<T> register(String name,
            BlockEntityType.BlockEntitySupplier<T> factory, Block... blocks) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(AcademyCraft.MODID, name),
            new BlockEntityType<>(factory, Set.of(blocks)));
    }

    public static void init() {}

}
