package cn.academy.block.tileentity;

import cn.academy.ACTiles;
import cn.academy.energy.IFConstants;
import cn.lambdalib2.multiblock.IMultiTile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * @author WeAthFolD
 */
public class TileSolarGen extends TileGeneratorBase implements IMultiTile {

    public static final int SLOT_BATTERY = 0;

    public enum SolarStatus {
        STOPPED, WEAK, STRONG
    }

    public TileSolarGen(BlockPos pos, BlockState state) {
        super(ACTiles.SOLAR_GEN, pos, state, "solar_generator", 1, 1000, IFConstants.LATENCY_MK2);
    }

    @Override
    public double getGeneration(double required) {
        Level world = this.getWorld();
        double brightLev =  canGenerate() ? 1.0 : 0.0;
        brightLev *= world.isRaining() ? 0.2 : 1.0;

        return Math.min(required, brightLev * 3.0);
    }

    public SolarStatus getStatus() {
        Level world = getWorld();
        if (canGenerate()) {
            return world.isRaining() ? SolarStatus.WEAK : SolarStatus.STRONG;
        } else {
            return SolarStatus.STOPPED;
        }
    }

    private boolean canGenerate() {
        Level world = getWorld();
        long time = world.getOverworldClockTime() % 24000;
        boolean isDay = time >= 0 && time <= 12500;
        return isDay && world.canSeeSky(getPos().above());
    }

    @Override
    public void update() {
        super.update();

        ItemStack battery = getStackInSlot(SLOT_BATTERY);
        if (!battery.isEmpty() && !getWorld().isClientSide()) {
            tryChargeStack(battery);
        }
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return cn.academy.energy.api.IFItemManager.instance.isSupported(stack);
    }

}
