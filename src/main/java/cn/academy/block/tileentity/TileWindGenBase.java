package cn.academy.block.tileentity;

import cn.academy.ACBlocks;
import cn.academy.ACTiles;
import cn.academy.block.WindGeneratorConsts;
import cn.academy.energy.IFConstants;
import cn.academy.energy.api.IFItemManager;
import cn.lambdalib2.multiblock.IMultiTile;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.TickScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * @author WeAthFolD
 */
public class TileWindGenBase extends TileGeneratorBase implements IMultiTile {

    public static double MAX_GENERATION_SPEED = 15;

    public enum Completeness {
        BASE_ONLY, NO_TOP, COMPLETE, COMPLETE_NOT_WORKING
    }

    private static final IFItemManager itemManager = IFItemManager.instance;

    // CLIENT STATES
    private TileWindGenMain mainTile;
    private boolean noObstacle;
    private Completeness completeness = Completeness.BASE_ONLY;

    private TickScheduler scheduler = new TickScheduler();

    {
        scheduler.every(10).run(() -> {
            updateMainTile();
            noObstacle = (mainTile != null && mainTile.noObstacle);
        });
    }

    public TileWindGenBase(BlockPos pos, BlockState state) {
        super(ACTiles.WINDGEN_BASE, pos, state, "windgen_base", 1, 20000, IFConstants.LATENCY_MK3);
    }

    @Override
    public double getGeneration(double required) {
        double sim = getSimulatedGeneration();
        return Math.min(required, sim);
    }

    // TODO: Improve the fomula?
    public double getSimulatedGeneration() {
        if(shouldGenerate()) {
            int y = mainTile.getPos().getY();
            double heightFactor = MathUtils.lerp(0.5, 1,
                MathUtils.clampd(0, 1, (y - 70.0) / 90.0));
            return heightFactor * MAX_GENERATION_SPEED;
        } else {
            return 0.0;
        }
    }

    private void updateChargeOut() {
        ItemStack stack = this.getStackInSlot(0);
        if (!stack.isEmpty()) {
            tryChargeStack(stack);
        }
    }

    @Override
    public void update() {
        super.update();
        scheduler.runTick();
        if (!getWorld().isClientSide()) {
            updateChargeOut();
        }
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return itemManager.isSupported(stack);
    }

    public boolean isComplete() {
        return completeness == Completeness.COMPLETE;
    }

    public Completeness getCompleteness() {
        if (completeness == Completeness.COMPLETE) {
            return shouldGenerate() ? Completeness.COMPLETE : Completeness.COMPLETE_NOT_WORKING;
        } else {
            return completeness;
        }
    }

    private boolean shouldGenerate() {
        return completeness == Completeness.COMPLETE && mainTile != null && mainTile.noObstacle && mainTile.isFanInstalled();
    }

    private void updateMainTile() {
        int pillars = 0;

        TileWindGenMain mainTile;
        Completeness comp;

        BlockPos p = getPos();
        for(int y = getPos().getY() + 2; ; ++y) {
            BlockPos pos = new BlockPos(p.getX(), y, p.getZ());
            if (!level.isInWorldBounds(pos)) {
                comp = pillars < WindGeneratorConsts.MIN_PILLARS ? Completeness.BASE_ONLY : Completeness.NO_TOP;
                mainTile = null;
                break;
            }
            BlockEntity te = level.getBlockEntity(pos);
            Block block = level.getBlockState(pos).getBlock();

            if(block == ACBlocks.windgen_pillar) {
                ++pillars;
                if(pillars > WindGeneratorConsts.MAX_PILLARS) {
                    comp = Completeness.NO_TOP;
                    mainTile = null;
                    break;
                }
            } else if(te instanceof TileWindGenMain gen) {
                if(gen.getBlockInfo().getSubID() == 0 && pillars >= WindGeneratorConsts.MIN_PILLARS) {
                    mainTile = gen;
                    comp = Completeness.COMPLETE;
                    break;
                } else {
                    comp = Completeness.NO_TOP;
                    mainTile = null;
                    break;
                }
            } else {
                comp = pillars < WindGeneratorConsts.MIN_PILLARS ? Completeness.BASE_ONLY : Completeness.NO_TOP;
                mainTile = null;
                break;
            }
        }

        this.mainTile = mainTile;
        this.completeness = comp;
    }
}
