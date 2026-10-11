package cn.lambdalib2.render.legacy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;

import java.util.function.Supplier;

/**
 * Registers a {@link LegacyRender} as a 26.x entity renderer.
 */
@Environment(EnvType.CLIENT)
public class LegacyRenderAdapter<T extends Entity> extends LegacyEntityRenderer<T> {

    private final LegacyRender<? super T> render;

    public LegacyRenderAdapter(EntityRendererProvider.Context ctx, LegacyRender<? super T> render) {
        super(ctx);
        this.render = render;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void doRender(T entity, double x, double y, double z, float partialTicks) {
        ((LegacyRender) render).doRender(entity, x, y, z, entity.getYRot(), partialTicks);
    }

    /**
     * @return A renderer provider creating an adapter around the render given by the supplier.
     */
    public static <T extends Entity> EntityRendererProvider<T> of(Supplier<? extends LegacyRender<? super T>> render) {
        return ctx -> new LegacyRenderAdapter<>(ctx, render.get());
    }

}
