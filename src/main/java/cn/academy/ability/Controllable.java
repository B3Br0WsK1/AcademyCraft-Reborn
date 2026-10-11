package cn.academy.ability;
import cn.academy.ability.context.ClientRuntime;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.s11n.nbt.NBTS11n;
import cn.lambdalib2.s11n.nbt.NBTS11n.BaseSerializer;
import cn.lambdalib2.s11n.network.NetworkS11n;
import cn.lambdalib2.s11n.network.NetworkS11n.ContextException;
import cn.lambdalib2.s11n.network.NetworkS11n.NetS11nAdaptor;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.resources.Identifier;
import net.fabricmc.api.EnvType;


/**
 * This class has ability to create a SkillInstance to override
 * a specific ability key. Used in Skill for indexing.
 * @author WeAthFolD
 *
 * SkillInstance has been removed.  --Paindar
 */
public abstract class Controllable {

    @StateEventCallback
    private static void init(StateEvents.Init ev) {
        NetworkS11n.addDirect(Controllable.class, new NetS11nAdaptor<Controllable>() {
            @Override
            public void write(ByteBuf buf, Controllable obj) {
                buf.writeByte(obj.getCategory().getCategoryID());
                buf.writeByte(obj.getControlID());
            }

            @Override
            public Controllable read(ByteBuf buf) throws ContextException {
                Category cat = CategoryManager.INSTANCE.getCategory(buf.readByte());
                return cat.getControllable(buf.readByte());
            }
        });
        NBTS11n.addBase(Controllable.class, new BaseSerializer<Tag, Controllable>() {
            @Override
            public Tag write(Controllable value) {
                return new ByteArrayTag(new byte[] {
                        (byte) value.getCategory().getCategoryID(),
                        (byte) value.getControlID()
                });
            }

            @Override
            public Controllable read(Tag tag, Class<? extends Controllable> type) {
                byte[] bytes = ((ByteArrayTag) tag).getAsByteArray();
                Category cat = CategoryManager.INSTANCE.getCategory(bytes[0]);
                return cat.getControllable(bytes[1]);
            }
        });
    }

    private Category category;
    private int id;
    
    public Controllable() {}
    
    final void addedControllable(Category _category, int _id) {
        category = _category;
        id = _id;
    }
    
    public final Category getCategory() {
        return category;
    }
    
    public final int getControlID() {
        return id;
    }

    /**
     * Invoked when given {@link Controllable} was activated. (e.g. The skill was in the switched preset),
     *  register the KeyDelegates into the runtime.
     *
     *  @param keyID The key ID associated with the skill currently
     */
    @Environment(EnvType.CLIENT)
    public /*abstract*/ void activate(ClientRuntime rt, int keyID) {}
    
    /**
     * Return the icon of this controllable. Used in KeyHint display UI.
     */
    public abstract Identifier getHintIcon();
    
    /**
     * Return the hint text of the controllable. Used in KeyHint display UI.
     */
    public abstract String getHintText();
    
    /**
     * @return Whether this controllable should override the vanilla key control.
     */
    public boolean shouldOverrideKey() {
        return true;
    }
    
}