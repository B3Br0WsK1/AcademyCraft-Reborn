package cn.academy.block.fluid;

import cn.academy.AcademyCraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * Fluids of AcademyCraft.
 */
public class ACFluids {

    public static final FlowingFluid IMAG_PHASE = Registry.register(BuiltInRegistries.FLUID,
        Identifier.fromNamespaceAndPath(AcademyCraft.MODID, "imag_phase"), new ImagPhaseFluid.Source());

    public static final FlowingFluid FLOWING_IMAG_PHASE = Registry.register(BuiltInRegistries.FLUID,
        Identifier.fromNamespaceAndPath(AcademyCraft.MODID, "flowing_imag_phase"), new ImagPhaseFluid.Flowing());

    public static void init() {}

}
