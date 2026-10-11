package cn.academy.ability.ctrl;

import cn.academy.ability.Skill;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.ContextManager;
import cn.academy.ability.context.DelegateState;
import cn.academy.ability.context.KeyDelegate;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.function.Function;

/**
 * Ported from Scala.
 */
public final class KeyDelegates {

    private KeyDelegates() {}

    /**
     * Creates a key delegate that activates the given context on key down, or terminates it if already active.
     * @param skill Skill that this context belongs to
     * @param klass type of context
     */
    public static <T extends Context<?>> KeyDelegate contextActivate(Skill skill, Class<T> klass,
                                                                  Function<Player, T> contextProvider) {
        return new KeyDelegate() {
            @Override
            public void onKeyDown() {
                Optional<T> ctx = findContext();
                if (ctx.isPresent()) {
                    ctx.get().terminate();
                } else {
                    ContextManager.instance.activate(contextProvider.apply(getPlayer()));
                }
            }

            @Override
            public int createID() {
                return 0;
            }

            @Override
            public Identifier getIcon() {
                return skill.getHintIcon();
            }

            @Override
            public Skill getSkill() {
                return skill;
            }

            @Override
            public DelegateState getState() {
                return findContext().isPresent() ? DelegateState.ACTIVE : DelegateState.IDLE;
            }

            private Optional<T> findContext() {
                return ContextManager.instance.findLocal(klass);
            }
        };
    }

}
