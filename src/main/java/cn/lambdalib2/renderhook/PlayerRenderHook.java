package cn.lambdalib2.renderhook;

import cn.lambdalib2.util.GameTimer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.player.Player;

/**
 * Register through DummyRenderData.get(player).addRenderHook(hook)
 * @author WeAthFolD
 */
@Environment(EnvType.CLIENT)
public abstract class PlayerRenderHook {

    Player player;
    boolean disposed;
    double createTime = GameTimer.getTime();

    public void renderHand(boolean firstPerson) {}

    public void dispose() {
        disposed = true;
    }

    public final Player getPlayer() {
        return player;
    }

    protected double getElapsedTime() {
        return GameTimer.getTime() - createTime;
    }

}
