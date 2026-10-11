package cn.lambdalib2;

import cn.lambdalib2.particle.Particle;
import cn.lambdalib2.particle.RenderParticle;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.renderhook.EntityDummy;
import cn.lambdalib2.renderhook.RenderDummy;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Entity types of LambdaLib2. These entities are client-local (never spawned on the server), but their types are
 *  registered on both sides to keep the registries in sync.
 */
public final class LLEntities {

    private LLEntities() {}

    public static EntityType<Particle> PARTICLE;
    public static EntityType<EntityDummy> DUMMY;

    @StateEventCallback(priority = 900)
    private static void preInit(StateEvents.PreInit ev) {
        PARTICLE = localType("particle", Particle::new);
        DUMMY = localType("render_dummy", EntityDummy::new);
    }

    /**
     * Registers an entity type used only for client side local entities.
     */
    public static <T extends Entity> EntityType<T> localType(String name, EntityType.EntityFactory<T> factory) {
        return register(Identifier.fromNamespaceAndPath("lambdalib2", name),
            EntityType.Builder.of(factory, MobCategory.MISC).sized(0.25f, 0.25f).noSave().noSummon()
                .clientTrackingRange(0).noLootTable());
    }

    public static <T extends Entity> EntityType<T> register(Identifier id, EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    @Environment(EnvType.CLIENT)
    @StateEventCallback
    private static void initClient(StateEvents.Init ev) {
        EntityRendererRegistry.register(PARTICLE, RenderParticle::new);
        EntityRendererRegistry.register(DUMMY, RenderDummy::new);
    }

}
