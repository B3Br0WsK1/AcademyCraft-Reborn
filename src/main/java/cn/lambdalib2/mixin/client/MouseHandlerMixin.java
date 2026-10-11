package cn.lambdalib2.mixin.client;

import cn.lambdalib2.input.MouseGrabHook;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @Shadow
    private double accumulatedDX;

    @Shadow
    private double accumulatedDY;

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void lambdalib2$onButton(long handle, net.minecraft.client.input.MouseButtonInfo info, int action, CallbackInfo ci) {
        if (cn.lambdalib2.input.RawInputHooks.fireMouseButton(info.button(), action == 1)) {
            ci.cancel();
        }
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void lambdalib2$onScroll(long handle, double xoffset, double yoffset, CallbackInfo ci) {
        if (cn.lambdalib2.input.RawInputHooks.fireScroll(yoffset)) {
            ci.cancel();
        }
    }

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void lambdalib2$grabMouse(double mousea, CallbackInfo ci) {
        MouseGrabHook.Handler h = MouseGrabHook.get();
        if (h != null) {
            h.onMouseMove(accumulatedDX, accumulatedDY);
            accumulatedDX = 0;
            accumulatedDY = 0;
            ci.cancel();
        }
    }

}
