package cn.lambdalib2.mixin.client;

import cn.lambdalib2.input.RawInputHooks;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void lambdalib2$keyPress(long handle, int action, KeyEvent event, CallbackInfo ci) {
        if (RawInputHooks.fireKey(event.key(), action != 0)) {
            ci.cancel();
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void lambdalib2$charTyped(long handle, CharacterEvent event, CallbackInfo ci) {
        if (RawInputHooks.fireChar(event.codepoint())) {
            ci.cancel();
        }
    }

}
