package cn.lambdalib2.util;

import cn.lambdalib2.render.legacy.GL11;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * This class essentially transforms the origin to the player's hand in thirdPerson or firstPerson.
 * @author WeAthFolD
 */
public class ViewOptimize {

    public interface IAssociatePlayer {
        Player getPlayer();
    }

    private static final double
        fpOffsetX = -0.05,
        fpOffsetY = -0.25,
        fpOffsetZ = 0.2;

    private static final double
        tpOffsetX = 0.15,
        tpOffsetY = -0.8,
        tpOffsetZ = 0.23;

    @Environment(EnvType.CLIENT)
    public static void fixFirstPerson() {
        GL11.glTranslated(fpOffsetX, fpOffsetY, fpOffsetZ);
    }

    @Environment(EnvType.CLIENT)
    public static void fixThirdPerson() {
        GL11.glTranslated(tpOffsetX, tpOffsetY, tpOffsetZ);
    }

    @Environment(EnvType.CLIENT)
    public static void fix(IAssociatePlayer entity) {
        if(isFirstPerson(entity)) {
            fixFirstPerson();
        } else {
            fixThirdPerson();
        }
    }

    @Environment(EnvType.CLIENT)
    public static Vec3 getFixVector(IAssociatePlayer entity) {
        if(isFirstPerson(entity)) {
            return new Vec3(fpOffsetX, fpOffsetY, fpOffsetZ);
        } else {
            return new Vec3(tpOffsetX, tpOffsetY, tpOffsetZ);
        }
    }

    @Environment(EnvType.CLIENT)
    public static boolean isFirstPerson(IAssociatePlayer entity) {
        return isFirstPerson(entity.getPlayer());
    }

    @Environment(EnvType.CLIENT)
    public static boolean isFirstPerson(Player player) {
        Minecraft mc = Minecraft.getInstance();
        return mc.options.getCameraType().isFirstPerson() && mc.player == player;
    }

}
