package cn.academy.client.sound;

import cn.academy.Resources;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Generic sound playing utils.
 * @author WeAthFolD
 */
public class ACSounds {

    @Environment(EnvType.CLIENT)
    public static void playClient(Entity target, String name, SoundSource category, float volume) {
        playClient(new FollowEntitySound(target, name, category).setVolume(volume));
    }

    @Environment(EnvType.CLIENT)
    public static void playClient(Level world, double x, double y, double z, String name, SoundSource category, float vol, float pitch) {
        world.playLocalSound(x, y, z, Resources.sound(name), category, vol, pitch, false);
    }

    @Environment(EnvType.CLIENT)
    public static void playClient(SoundInstance sound) {
        Minecraft.getInstance().getSoundManager().play(sound);
    }

    /**
     * Plays a sound at the given position for all players nearby. Server side.
     */
    public static void playServer(Level world, double x, double y, double z, String name, SoundSource category, float vol, float pitch) {
        world.playSound(null, x, y, z, Resources.sound(name), category, vol, pitch);
    }

}
