package cn.lambdalib2.util;

import cn.lambdalib2.auxgui.AuxGuiHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

@Environment(EnvType.CLIENT)
public class ClientUtils {

    /**
     * @return whether the player is playing the client game and isn't opening any GUI.
     */
    public static boolean isPlayerInGame() {
        Player player = Minecraft.getInstance().player;
        return player != null && Minecraft.getInstance().gui.screen() == null && !AuxGuiHandler.hasForegroundGui();
    }

    public static boolean isInWorld() {
        return Minecraft.getInstance().player != null;
    }

    public static boolean isPlayerPlaying() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && !mc.isPaused();
    }

    /**
     * Quick alias for playing static sound
     */
    public static void playSound(Identifier src, float pitch) {
        Minecraft.getInstance().getSoundManager().play(
            SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(src), pitch));
    }

    public static String getClipboardContent() {
        return Minecraft.getInstance().keyboardHandler.getClipboard();
    }

    public static void setClipboardContent(String content) {
        Minecraft.getInstance().keyboardHandler.setClipboard(content);
    }

}
