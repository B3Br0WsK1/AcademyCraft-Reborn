package cn.lambdalib2.mixin.client;

import cn.lambdalib2.render.HandRenderHooks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonHandsMixin {

    @Inject(method = "submitHandsWithItems", at = @At("HEAD"))
    private void lambdalib2$transformHands(float partialTicks, PoseStack poseStack, SubmitNodeCollector collector,
                                           PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState state,
                                           CallbackInfo ci) {
        Matrix4f m = HandRenderHooks.getTransform(partialTicks);
        if (m != null) {
            poseStack.mulPose(m);
        }
    }

}
