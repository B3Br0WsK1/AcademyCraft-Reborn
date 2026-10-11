package cn.lambdalib2.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * Allows spawning client-local entities with {@code level.addFreshEntity} on the client, like 1.12
 *  {@code world.spawnEntity} did. Local entities get negative ids so they never collide with server entities.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    @Unique
    private static int lambdalib2$nextLocalId = -1000;

    @Shadow
    public abstract void addEntity(Entity entity);

    public boolean addFreshEntity(Entity entity) {
        entity.setId(lambdalib2$nextLocalId--);
        if (lambdalib2$nextLocalId < -1_000_000_000) {
            lambdalib2$nextLocalId = -1000;
        }
        addEntity(entity);
        return true;
    }

}
