package cn.academy.terminal.app;
import cn.academy.client.auxgui.FreqTransmitterUI;
import cn.academy.terminal.App;
import cn.academy.terminal.AppEnvironment;
import cn.academy.client.auxgui.TerminalUI;
import cn.academy.terminal.RegApp;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;


/**
 * @author WeAthFolD
 */
public class AppFreqTransmitter extends App {
    
    @RegApp
    public static AppFreqTransmitter instance = new AppFreqTransmitter();

    private AppFreqTransmitter() {
        super("freq_transmitter");
    }

    @Override
    @Environment(EnvType.CLIENT)
    public AppEnvironment createEnvironment() {
        return new AppEnvironment() {
            @Override
            @Environment(EnvType.CLIENT)
            public void onStart() {
                TerminalUI.passOn(new FreqTransmitterUI());
            }
        };
    }

}