package cn.academy.block.tileentity;

import cn.academy.ACBlocks;
import cn.academy.ACItems;
import cn.academy.ACTiles;
import cn.academy.block.WindGeneratorConsts;
import cn.lambdalib2.multiblock.BlockMulti;
import cn.lambdalib2.multiblock.BlockMulti.SubBlockPos;
import cn.lambdalib2.multiblock.IMultiTile;
import cn.lambdalib2.multiblock.InfoBlockMulti;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.NetworkMessage.NullablePar;
import cn.lambdalib2.s11n.network.TargetPoints;
import cn.lambdalib2.template.tile.ITickableTile;
import cn.lambdalib2.util.Side;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * @author WeAthFolD
 */
public class TileWindGenMain extends TileInventory implements IMultiTile, ITickableTile {

    @SuppressWarnings("unchecked")
    static List<SubBlockPos>[] checkAreas = new ArrayList[6];
    static {
        List<SubBlockPos> checkArea = new ArrayList<>();
        for(int i = -7; i <= 7; ++i) {
            for(int j = -7; j <= 7; ++j) {
                if(i != 0 || j != 0)
                    checkArea.add(new SubBlockPos(i, j, -1));
            }
        }

        for(int i = 2; i < 6; ++i) {
            List<SubBlockPos> list = (checkAreas[i] = new ArrayList<>());
            for(SubBlockPos pos : checkArea) {
                list.add(BlockMulti.rotate(pos, Direction.values()[i]));
            }
        }
    }

    public double lastFrame = -1;
    public float lastRotation;

    public boolean complete;
    public boolean noObstacle;

    int updateWait, updateWait2;

    public TileWindGenMain(BlockPos pos, BlockState state) {
        super(ACTiles.WINDGEN_MAIN, pos, state, "windgen_main", 1);
    }

    // Spin logic
    public boolean isFanInstalled() {
        ItemStack stack = this.getStackInSlot(0);
        return !stack.isEmpty() && stack.getItem() == ACItems.windgen_fan;
    }

    /**
     * Unit: Degree per second
     */
    public double getSpinSpeed() {
        return complete ? 60.0 : 0;
    }

    @Override
    public void update() {
        InfoBlockMulti info = getBlockInfo();

        if(info.getSubID() == 0) {
            if(++updateWait == 10) {
                updateWait = 0;
                complete = isCompleteStructure();
                noObstacle = complete && isNoObstacle();
            }

            if(!getWorld().isClientSide()) {
                if(++updateWait2 == 20) {
                    updateWait2 = 0;
                    NetworkMessage.sendToAllAround(
                            TargetPoints.convert(this, 50),
                            this, "sync", inventory[0]
                    );
                }
            }
        }
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot != 0 || (stack.getItem() == ACItems.windgen_fan);
    }

    public boolean isCompleteStructure() {
        BlockPos origin = ACBlocks.windgen_main.getOrigin(this);
        if(origin == null)
            return false;

        int x = origin.getX(), y = origin.getY() - 1, z = origin.getZ();
        int state = 1;
        int pillars = 0;

        for(; state < 2; --y) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.isInWorldBounds(pos)) {
                state = 3;
                break;
            }
            Block block = level.getBlockState(pos).getBlock();
            if(block == ACBlocks.windgen_pillar) {
                ++pillars;
                if(pillars > WindGeneratorConsts.MAX_PILLARS)
                    break;
            } else if(block == ACBlocks.windgen_base){
                state = 2;
            } else {
                state = 3;
            }
        }
        return state == 2 && pillars >= WindGeneratorConsts.MIN_PILLARS;
    }

    public boolean isNoObstacle() {
        InfoBlockMulti info = getBlockInfo();
        Level world = getWorld();
        List<SubBlockPos> arr = checkAreas[info.getDir().ordinal()];
        for(SubBlockPos sbp : arr) {
            if(!world.getBlockState(getPos().offset(sbp.dx, sbp.dy, sbp.dz)).isAir())
                return false;
        }
        return true;
    }

    @Listener(channel="sync", side=Side.CLIENT)
    private void syncStack(@NullablePar ItemStack stack) {
        setInventorySlotContents(0, stack == null ? ItemStack.EMPTY : stack);
    }

}
