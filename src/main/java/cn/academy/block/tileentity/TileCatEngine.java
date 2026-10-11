package cn.academy.block.tileentity;

import cn.academy.ACTiles;
import cn.lambdalib2.util.TickScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Cat Engine!
 * @author WeAthFolD
 */
public class TileCatEngine extends TileGeneratorBase {

    private final TickScheduler scheduler = new TickScheduler();

    // Sync
    int syncTicker;

    // Intrusive render parameters
    public double thisTickGen;
    public double rotation;
    public long lastRender;

    public TileCatEngine(BlockPos pos, BlockState state) {
        super(ACTiles.CAT_ENGINE, pos, state, "infinite_generator", 0, 2000, 200);
    }

    @Override
    public void update() {
        super.update();
        scheduler.runTick();
    }

    @Override
    public double getGeneration(double required) {
        return (thisTickGen = Math.min(required, 500));
    }

    @Override
    public CompoundTag writeToNBT(CompoundTag tag)
    {
        super.writeToNBT(tag);
        tag.putDouble("tickGen", thisTickGen);
        return tag;
    }

    @Override
    public void readFromNBT(CompoundTag tag)
    {
        super.readFromNBT(tag);
        thisTickGen = tag.getDoubleOr("tickGen", 0);
    }
}
