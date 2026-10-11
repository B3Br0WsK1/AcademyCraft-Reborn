package cn.lambdalib2.multiblock;

import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Marks the block entity of a {@link BlockMulti} origin block.
 * @author WeathFolD
 */
public interface IMultiTile {

    default InfoBlockMulti getBlockInfo() {
        return InfoBlockMulti.of((BlockEntity) this);
    }

}
