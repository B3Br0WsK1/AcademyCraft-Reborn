package cn.academy.worldgen;
import cn.academy.AcademyCraft;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;


/**
 * World generation of AcademyCraft. Ores and phase liquid lakes are data driven features
 *  (data/academy/worldgen/{feature,placed_feature}) that are added to the overworld biomes here.
 *
 * @author WeAthFolD, Shielian, KS
 */
public class WorldGenInit {

    // CONFIGS
    public static boolean GENERATE_ORES, GENERATE_PHASE_LIQUID;

    private static final String[] ORES = { "reso_ore", "constraint_metal", "crystal_ore", "imagsil_ore" };

    private static ResourceKey<PlacedFeature> placed(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath("academy", name));
    }

    @StateEventCallback
    private static void init(StateEvents.Init event) {
        GENERATE_ORES = AcademyCraft.config.getBoolean("genOres", "generic", true, "Whether the ores will be generated in overworld.");
        GENERATE_PHASE_LIQUID = AcademyCraft.config.getBoolean("genPhaseLiquid", "generic", true, "Whether phase liquid will be generated in overworld.");

        if (GENERATE_ORES) {
            for (String ore : ORES) {
                BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                    GenerationStep.Decoration.UNDERGROUND_ORES, placed(ore));
            }
        }
        if (GENERATE_PHASE_LIQUID) {
            BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.LAKES, placed("phase_lake"));
        }
    }

}
