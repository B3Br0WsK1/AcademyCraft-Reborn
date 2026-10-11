package cn.lambdalib2.event.mc;

import cn.lambdalib2.event.Event;
import cn.lambdalib2.util.Side;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

/**
 * Tick events, fired at the START and END of a tick.
 */
public class TickEvent extends Event {

    public enum Phase { START, END }

    public final Phase phase;
    public final Side side;

    public TickEvent(Side side, Phase phase) {
        this.side = side;
        this.phase = phase;
    }

    public static class ClientTickEvent extends TickEvent {
        public ClientTickEvent(Phase phase) {
            super(Side.CLIENT, phase);
        }
    }

    public static class ServerTickEvent extends TickEvent {
        public final MinecraftServer server;

        public ServerTickEvent(MinecraftServer server, Phase phase) {
            super(Side.SERVER, phase);
            this.server = server;
        }
    }

    public static class PlayerTickEvent extends TickEvent {
        public final Player player;

        public PlayerTickEvent(Phase phase, Player player) {
            super(player.level().isClientSide() ? Side.CLIENT : Side.SERVER, phase);
            this.player = player;
        }
    }

    /**
     * Fired every render frame on client (START before world rendering, END after GUI rendering).
     */
    public static class RenderTickEvent extends TickEvent {
        public final float renderTickTime;

        public RenderTickEvent(Phase phase, float renderTickTime) {
            super(Side.CLIENT, phase);
            this.renderTickTime = renderTickTime;
        }
    }
}
