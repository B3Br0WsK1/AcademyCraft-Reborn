package cn.academy.client.render.block;

import cn.academy.block.fluid.ACFluids;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;

/**
 * Client rendering of the imaginary phase liquid (replacement of the forge:fluid model).
 */
@Environment(EnvType.CLIENT)
public final class ACFluidRenderers {

    private ACFluidRenderers() {}

    @StateEventCallback
    private static void init(StateEvents.Init ev) {
        Identifier tex = Identifier.fromNamespaceAndPath("academy", "block/phase_liquid");
        Material mat = new Material(tex, true);
        FluidRenderingRegistry.register(ACFluids.IMAG_PHASE, ACFluids.FLOWING_IMAG_PHASE,
            new FluidModel.Unbaked(mat, mat, null, null));
    }

}
