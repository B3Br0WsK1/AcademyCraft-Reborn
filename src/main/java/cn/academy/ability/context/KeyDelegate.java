package cn.academy.ability.context;
import cn.academy.ability.Skill;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.Identifier;
import net.fabricmc.api.EnvType;

//import net.minecraftforge.fml.common.registry.RegistryDelegate.Delegate;

/**
 * {@link KeyDelegate} is provided by a skill and handles key delegation of ONE key.
 * @author WeAthFolD
 */
@Environment(EnvType.CLIENT)
public abstract class KeyDelegate {

    private Integer identifier = null;

    public void onKeyDown() {}

    public void onKeyUp() {}

    public void onKeyAbort() {}

    public void onKeyTick() {}

    protected final Minecraft getMC() {
        return Minecraft.getInstance();
    }

    protected final Player getPlayer() {
        return getMC().player;
    }

    /**
     * @return The icon displayed in the key hint UI.
     */
    public abstract Identifier getIcon();

    /**
     * @return The skill subID used in cooldown
     */
    public abstract int createID();

    /**
     * @return The skill that this delegate belongs to
     */
    public abstract Skill getSkill();

    /**
     * @return The identifier of this KeyDelegate used in cooldown
     */
    public final Integer getIdentifier() {
        if (identifier == null) {
            identifier = createID();
        }
        return identifier;
    }

    public DelegateState getState() {
        return DelegateState.IDLE;
    }

}