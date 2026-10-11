package cn.academy.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The render camera position in the render coordinate space. Rendering (of legacy renderers) happens relative to the
 *  camera, so the camera is always at the origin.
 */
@Environment(EnvType.CLIENT)
public class CameraPosition {

    private CameraPosition() {}

    private static final Vec3 zero = new Vec3(0, 0, 0);

    public static Vector3f get() {
        return new Vector3f();
    }

    public static Vec3 getVec3d() {
        return zero;
    }

}
