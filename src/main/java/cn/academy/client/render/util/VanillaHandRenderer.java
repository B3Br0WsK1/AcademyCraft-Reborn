package cn.academy.client.render.util;

import cn.academy.datapart.HandRenderOverrideData;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.render.HandRenderHooks;
import cn.lambdalib2.vis.CompTransform;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;

/**
 * Renders the vanilla hand with an extra transform. In 26.x the vanilla hands are drawn as usual and the transform
 *  set by {@link #renderHand(float, CompTransform)} (called by the active {@link IHandRenderer}) is applied to them.
 */
@Environment(EnvType.CLIENT)
public class VanillaHandRenderer {

    private static Matrix4f pending;

    public static void renderHand(float partialTicks) {
        renderHand(partialTicks, CompTransform.identity);
    }

    public static void renderHand(float partialTicks, CompTransform transform) {
        pending = transform.toMatrix();
    }

    @StateEventCallback
    private static void init(StateEvents.Init ev) {
        HandRenderHooks.register(partialTicks -> {
            Player player = Minecraft.getInstance().player;
            if (player == null) return null;
            HandRenderOverrideData data = HandRenderOverrideData.get(player);
            if (data == null || !data.isRendererPresent()) return null;
            pending = null;
            data.getRenderer().renderHand(partialTicks);
            Matrix4f ret = pending;
            pending = null;
            return ret;
        });
    }

}
