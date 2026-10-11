package cn.academy.ability.vanilla.util;
import cn.lambdalib2.event.SubscribeEvent;
import cn.lambdalib2.event.mc.TickEvent;


public abstract class HandlerLifePeroidEvent
{
    protected int maxTick = 0;
    protected int tick = 0;
    public HandlerLifePeroidEvent(int maxTick)
    {
        this.maxTick = maxTick;
    }

    public abstract boolean onTick();
    public abstract void onDeath();
    public void setTick(int tick){
        this.tick = tick;
    }
    public void setMaxTick(int maxTick){
        this.maxTick = maxTick;
    }

    @SubscribeEvent
    public void onEvent(TickEvent.ClientTickEvent event){
        if(event.phase == TickEvent.Phase.END)
            return;
        if(tick >= maxTick){
            onDeath();
        }
        else{
            onTick();
            tick++;
        }
    }

}
