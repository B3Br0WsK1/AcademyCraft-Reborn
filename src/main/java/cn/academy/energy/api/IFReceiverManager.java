package cn.academy.energy.api;
import cn.academy.energy.api.block.IWirelessReceiver;
import cn.academy.support.EnergyBlockHelper;
import cn.academy.support.EnergyBlockHelper.IEnergyBlockManager;
import cn.lambdalib2.registry.StateEventCallback;
import net.minecraft.world.level.block.entity.BlockEntity;
import cn.lambdalib2.registry.StateEvents;


/**
 * @author WeAthFolD
 */
public class IFReceiverManager implements IEnergyBlockManager {

    public static IFReceiverManager instance = new IFReceiverManager();

    @StateEventCallback
    private static void init(StateEvents.Init ev) {
        EnergyBlockHelper.register(instance);
    }

    private IFReceiverManager() {
    }

    @Override
    public boolean isSupported(BlockEntity tile) {
        return tile instanceof IWirelessReceiver;
    }

    @Override
    public double getEnergy(BlockEntity tile) {
        return 0;
    }

    @Override
    public void setEnergy(BlockEntity tile, double energy) {
        // throw new UnsupportedOperationException();
    }

    @Override
    public double charge(BlockEntity tile, double amt, boolean ignoreBandwidth) {
        return ((IWirelessReceiver) tile).injectEnergy(amt);
    }

    @Override
    public double pull(BlockEntity tile, double amt, boolean ignoreBandwidth) {
        return ((IWirelessReceiver) tile).pullEnergy(amt);
    }

}