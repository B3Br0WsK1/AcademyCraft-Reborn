package cn.lambdalib2.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Allows constructing entities without a level (e.g. particle templates of LambdaLib2), as in 1.12.
 */
@Mixin(Entity.class)
public abstract class EntityTemplateMixin {

    @Redirect(method = "<init>", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/Level;getNextEntityId()I"))
    private int lambdalib2$nextId(Level level) {
        return level == null ? 0 : level.getNextEntityId();
    }

}
