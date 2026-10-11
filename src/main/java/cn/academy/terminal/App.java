package cn.academy.terminal;
import cn.academy.Resources;
import cn.academy.util.ACI18n;
import net.minecraft.resources.Identifier;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;


/**
 */
public abstract class App {

    int appid;
    private final String name;
    protected Identifier icon;

    private boolean preInstalled = false;

    public App(String _name) {
        name = _name;
        icon = getTexture("icon");
    }

    protected Identifier getTexture(String texname) {
        return Resources.getTexture("guis/apps/" + name + "/" + texname);
    }

    private String localKey(String key) {
        return "ac.app." + name + "." + key;
    }

    protected String local(String key) {
        return ACI18n.translateFormatted(localKey(key));
    }

    @Environment(EnvType.CLIENT)
    public Identifier getIcon() {
        return icon;
    }

    public App setPreInstalled() {
        preInstalled = true;
        return this;
    }

    public int getID() {
        return appid;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return local("name");
    }

    public String getDisplayKey() {
        return localKey("name");
    }

    public final boolean isPreInstalled() {
        return preInstalled;
    }

    @Environment(EnvType.CLIENT)
    public abstract AppEnvironment createEnvironment();

}