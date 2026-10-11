package cn.academy.block.block;

import cn.academy.ability.develop.DeveloperType;
import cn.academy.block.tileentity.TileDeveloper;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * @author WeAthFolD
 *
 */
public class BlockDeveloper extends ACBlockMulti {

    public final DeveloperType type;

    public BlockDeveloper(DeveloperType _type, Properties props) {
        super(props);
        type = _type;

        this.addSubBlock(0, 1, 0);
        this.addSubBlock(0, 0, 1);
        this.addSubBlock(0, 1, 1);
        this.addSubBlock(0, 2, 1);
        this.addSubBlock(0, 0, 2);
        this.addSubBlock(0, 1, 2);
        this.addSubBlock(0, 2, 2);

        finishInit();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        BlockEntity te = getOriginTile(world, pos);
        if(te instanceof TileDeveloper td && !player.isShiftKeyDown()) {
            if(!world.isClientSide()) {
                if(td.getUser() == null) {
                    td.use(player);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public BlockEntity createNewTileEntity(BlockPos pos, BlockState state) {
        return type == DeveloperType.NORMAL ? new TileDeveloper.Normal(pos, state) : new TileDeveloper.Advanced(pos, state);
    }

    @Override
    public double[] getRotCenter() {
        return new double[] { 0.5, 0, 0.5 };
    }

    @Environment(EnvType.CLIENT)
    @StateEventCallback
    private static void initClient(StateEvents.Init ev) {
        // Hide the block highlight of developers
        LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, outline) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return true;
            return !(mc.level.getBlockState(outline.pos()).getBlock() instanceof BlockDeveloper);
        });
    }

}
