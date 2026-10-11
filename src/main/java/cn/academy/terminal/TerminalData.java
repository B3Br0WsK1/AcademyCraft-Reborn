package cn.academy.terminal;
import cn.academy.event.AppInstalledEvent;
import cn.academy.event.TerminalInstalledEvent;
import cn.lambdalib2.s11n.SerializeIncluded;
import cn.lambdalib2.s11n.nbt.NBTS11n;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.datapart.DataPart;
import cn.lambdalib2.datapart.EntityData;
import cn.lambdalib2.datapart.RegDataPart;
import cn.lambdalib2.util.Side;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import cn.lambdalib2.event.EventHub;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;



/**
 * @author WeAthFolD
 */
@RegDataPart(Player.class)
public class TerminalData extends DataPart<Player> {

    public static TerminalData get(Player player) {
        return EntityData.get(player).getPart(TerminalData.class);
    }

    @SerializeIncluded
    private List<Integer> installedNameHashes = new ArrayList<>();
    @SerializeIncluded
    private boolean isInstalled;

    public TerminalData() {
        setClientNeedSync();
        setNBTStorage();
    }

    public List<App> getInstalledApps() {
        return AppRegistry.enumeration().stream().filter(this::isInstalled).collect(Collectors.toList());
    }

    public boolean isInstalled(App app) {
        return app.isPreInstalled() || installedNameHashes.contains(app.getName().hashCode());
    }

    public boolean isTerminalInstalled() {
        return isInstalled;
    }

    /**
     * Server only. Installs the data terminal.
     */
    public void install() {
        checkSide(Side.SERVER);

        if (!isInstalled) {
            isInstalled = true;

            sync();

            informTerminalInstall();
            NetworkMessage.sendTo(getEntity(), this, "terminal_inst");
        }
    }

    /**
     * Server only. Installs the given app.
     */
    public void installApp(App app) {
        checkSide(Side.SERVER);

        if (!isInstalled(app)) {
            installedNameHashes.add(app.getName().hashCode());

            sync();

            informAppInstall(app.getName());
            NetworkMessage.sendTo(getEntity(), this, "app_inst", app.getName());
        }
    }

    @Override
    public void fromNBT(CompoundTag tag) {
        NBTS11n.read(tag, this);
    }

    @Override
    public void toNBT(CompoundTag tag) {
        NBTS11n.write(tag, this);
    }

    @Listener(channel="terminal_inst", side=Side.CLIENT)
    private void informTerminalInstall() {
        EventHub.BUS.post(new TerminalInstalledEvent(getEntity()));
    }

    @Listener(channel="app_inst", side=Side.CLIENT)
    private void informAppInstall(String appName) {
        EventHub.BUS.post(new AppInstalledEvent(getEntity(), AppRegistry.getByName(appName)));
    }

}