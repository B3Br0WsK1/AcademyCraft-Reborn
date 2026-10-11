package cn.academy.ability.vanilla.teleporter.util;
import cn.academy.ability.vanilla.util.HandlerLifePeroidEvent;
import net.minecraft.world.entity.player.Player;
import cn.lambdalib2.event.EventHub;
import cn.lambdalib2.event.SubscribeEvent;
import cn.lambdalib2.event.mc.TickEvent;


/**
 * @author WeAthFolD
 */
public class GravityCancellor extends HandlerLifePeroidEvent
{
    private final Player p;

    public GravityCancellor(Player _p, int _ticks) {
        super(_ticks);
        p = _p;
    }

    public final boolean isDead() {
        return tick >= maxTick;
    }

    public final void setDead() {
        this.tick = maxTick;
    }

    public final void reset() {
        this.tick = 0;
    }

    @Override
    public boolean onTick() {
        if (!p.getAbilities().flying) {
            if (!p.onGround()) {
                p.setDeltaMovement(p.getDeltaMovement().x, p.getDeltaMovement().y + (0.072), p.getDeltaMovement().z);
            }
        }
        return true;
    }

    @Override
    public void onDeath(){
        EventHub.BUS.unregister(this);
    }

}