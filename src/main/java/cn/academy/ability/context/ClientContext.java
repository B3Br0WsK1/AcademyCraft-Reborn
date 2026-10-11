package cn.academy.ability.context;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import java.util.function.Function;
import net.fabricmc.api.EnvType;



/**
 * {@link ClientContext} is attached to a certain context and receives all its messages.
 * @see RegClientContext
 */
@Environment(EnvType.CLIENT)
public class ClientContext extends Context {

    // RegClientContext support
    static final Multimap<Class<? extends Context>, Function<Context, ClientContext>>
            clientTypes = HashMultimap.create();

    public final Context parent;

    /**
     * This constructor must be kept in subclasses in order for reflection to work.
     */
    public ClientContext(Context _parent) {
        super(_parent.player, _parent.skill);
        parent = _parent;
    }

    @Override
    public Context.Status getStatus() {
        return parent.status;
    }

    @Override
    public void terminate() {
        parent.terminate();
    }
}