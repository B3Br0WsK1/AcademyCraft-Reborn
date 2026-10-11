package cn.academy.entity;

import cn.academy.ACEntities;
import cn.lambdalib2.util.entityx.EntityAdvanced;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.level.Level;

/**
 * Entities that are only spawned in client and is not persistent. Used to reduce syntax overhead.
 *  They are all rendered by {@link cn.academy.client.render.entity.LocalEffectRender}, which delegates to
 *  {@link #renderEffect}.
 * @author WeAthFolD
 */
public class LocalEntity extends EntityAdvanced {

    public LocalEntity(Level world) {
        super(ACEntities.LOCAL_EFFECT, world);
        this.noPhysics = true;
    }

    /**
     * Renders this effect through the legacy GL emulation. (x, y, z) is the entity position relative to the camera,
     *  and the modelview matrix is at the camera.
     */
    @Environment(EnvType.CLIENT)
    public void renderEffect(double x, double y, double z, float partialTicks) {}

}
