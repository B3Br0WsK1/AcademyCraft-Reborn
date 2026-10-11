package cn.lambdalib2.input;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

@Environment(EnvType.CLIENT)
public class KeyHandler {

    public void onKeyDown() {}

    public void onKeyUp() {}

    /**
     * This happens when the KeyBinding is a non-global one,
     * and player opens any GUI or jumps out of the game.
     */
    public void onKeyAbort() {}

    public void onKeyTick() {}

    protected Minecraft getMC() {
        return Minecraft.getInstance();
    }

    protected Player getPlayer() {
        return getMC().player;
    }

}
