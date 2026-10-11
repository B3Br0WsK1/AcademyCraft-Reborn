package cn.lambdalib2.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * @author WeAthFolD
 */
public interface ISpriteEntity {

    /**
     * Called each rendering frame before rendering to update the sprite's state
     */
    @Environment(EnvType.CLIENT)
    void updateSprite(Sprite s);

    boolean needViewOptimize();

}
