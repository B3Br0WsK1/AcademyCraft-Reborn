package cn.lambdalib2.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Hooks to transform the first person hands rendering (replacement of the 1.12 RenderHandEvent cancel-and-redraw
 *  approach).
 */
@Environment(EnvType.CLIENT)
public final class HandRenderHooks {

    private HandRenderHooks() {}

    public interface HandTransformProvider {
        /**
         * @return A transform to apply to the hands this frame, or null to leave them untouched
         */
        @Nullable Matrix4f getHandTransform(float partialTicks);
    }

    private static final List<HandTransformProvider> providers = new ArrayList<>();

    public static void register(HandTransformProvider p) {
        providers.add(p);
    }

    public static @Nullable Matrix4f getTransform(float partialTicks) {
        for (HandTransformProvider p : providers) {
            Matrix4f m = p.getHandTransform(partialTicks);
            if (m != null) return m;
        }
        return null;
    }

}
