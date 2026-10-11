package cn.academy.client.render.block;

import cn.academy.ACTiles;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import cn.lambdalib2.render.legacy.LegacyTileRenderer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import java.util.function.Supplier;

/**
 * Registers the block entity renderers.
 */
@Environment(EnvType.CLIENT)
public final class ACTileRenderers {

    private ACTileRenderers() {}

    @StateEventCallback
    private static void init(StateEvents.Init ev) {
        reg(ACTiles.CAT_ENGINE, () -> new RenderCatEngine());
        reg(ACTiles.DEVELOPER_NORMAL, () -> new RenderDeveloperNormal());
        reg(ACTiles.DEVELOPER_ADVANCED, () -> new RenderDeveloperAdvanced());
        reg(ACTiles.IMAG_PHASE, () -> new RenderImagPhaseLiquid());
        reg(ACTiles.MATRIX, () -> new RenderMatrix());
        reg(ACTiles.PHASE_GEN, () -> new RenderPhaseGen());
        reg(ACTiles.SOLAR_GEN, () -> new RenderSolarGen());
        reg(ACTiles.WINDGEN_BASE, () -> new RenderWindGenBase());
        reg(ACTiles.WINDGEN_MAIN, () -> new RenderWindGenMain());
        reg(ACTiles.WINDGEN_PILLAR, () -> new RenderWindGenPillar());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void reg(BlockEntityType type, Supplier<? extends LegacyTileRenderer> renderer) {
        BlockEntityRendererRegistry.register(type, ctx -> renderer.get());
    }

}
