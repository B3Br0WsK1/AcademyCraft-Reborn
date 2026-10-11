package cn.academy.event.ability;
import cn.academy.ability.Skill;
import cn.lambdalib2.event.Event;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import cn.lambdalib2.event.EventHub;


public class CalcEvent<T> extends Event {

    /**
     * Post the specified event to the event bus and returns the result.
     *  A fast and common routine to perform calculation.
     */
    public static <T> T calc(CalcEvent<T> evt) {
        EventHub.BUS.post(evt);
        return evt.value;
    }

    public T value;

    public CalcEvent(T initial) {
        value = initial;
    }

    public static class PlayerCalcEvent<T> extends CalcEvent<T> {

        public final Player player;

        public PlayerCalcEvent(Player _player, T initial) {
            super(initial);
            player = _player;
        }

    }

    public static class MaxCP extends PlayerCalcEvent<Float> {
        public MaxCP(Player player, float initial) {
            super(player, initial);
        }
    }

    public static class CPRecoverSpeed extends PlayerCalcEvent<Float> {
        public CPRecoverSpeed(Player player, float initial) {
            super(player, initial);
        }
    }

    public static class OverloadRecoverSpeed extends PlayerCalcEvent<Float> {
        public OverloadRecoverSpeed(Player player, float initial) {
            super(player, initial);
        }
    }

    public static class MaxOverload extends PlayerCalcEvent<Float> {
        public MaxOverload(Player player, float initial) {
            super(player, initial);
        }
    }

    public static class SkillAttack extends PlayerCalcEvent<Float> {

        public final Skill skill;
        public final Entity target;

        public SkillAttack(Player player, Skill _skill, Entity _target, float initial) {
            super(player, initial);
            skill = _skill;
            target = _target;
        }

    }

    public static class SkillPerform extends AbilityEvent {

        public float cp;
        public float overload;

        public SkillPerform(Player player, float _overload, float _cp) {
            super(player);

            cp = _cp;
            overload = _overload;
        }

    }

}