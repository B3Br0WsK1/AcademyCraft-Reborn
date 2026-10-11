package cn.academy.client.render.entity;

import cn.academy.entity.LocalEntity;
import cn.lambdalib2.render.legacy.LegacyRender;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.Entity;

/**
 * Renders {@link LocalEntity} effects by delegating to the entity.
 */
@Environment(EnvType.CLIENT)
public class LocalEffectRender extends LegacyRender<Entity> {

    @Override
    public void doRender(Entity entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (entity instanceof LocalEntity local) {
            local.renderEffect(x, y, z, partialTicks);
        }
    }

}
