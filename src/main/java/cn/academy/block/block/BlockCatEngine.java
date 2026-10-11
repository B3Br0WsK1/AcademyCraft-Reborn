package cn.academy.block.block;

import cn.academy.block.tileentity.TileCatEngine;
import cn.academy.energy.api.WirelessHelper;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.event.energy.LinkUserEvent;
import cn.academy.event.energy.UnlinkUserEvent;
import cn.lambdalib2.event.EventHub;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/**
 * @author WeAthFolD
 */
public class BlockCatEngine extends ACBlockContainer {

    public BlockCatEngine(Properties props) {
        super(props, null);
    }

    @Override
    public BlockEntity createNewTileEntity(BlockPos pos, BlockState state) {
        return new TileCatEngine(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        BlockEntity te = world.getBlockEntity(pos);
        if(te instanceof TileCatEngine gen) {
            if(!world.isClientSide()) {
                if(WirelessHelper.isGeneratorLinked(gen)) {
                    EventHub.BUS.post(new UnlinkUserEvent(gen));
                    player.sendSystemMessage(Component.translatable("ac.cat_engine.unlink"));
                } else {
                    List<IWirelessNode> nodes = WirelessHelper.getNodesInRange(world, pos);
                    if(nodes.isEmpty()) {
                        player.sendSystemMessage(Component.translatable("ac.cat_engine.notfound"));
                    } else {
                        IWirelessNode node = nodes.get(RandUtils.rangei(0, nodes.size()));
                        player.sendSystemMessage(Component.translatable("ac.cat_engine.linked", node.getNodeName()));
                        EventHub.BUS.post(new LinkUserEvent(gen, node));
                    }
                }
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

}
