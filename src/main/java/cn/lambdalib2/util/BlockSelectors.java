/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of LambdaLib modding library.
* https://github.com/LambdaInnovation/LambdaLib
* Licensed under MIT, see project root for more information.
*/
package cn.lambdalib2.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BlockSelectors {

    public static final IBlockSelector

    filNothing = (world, x, y, z, block) -> !world.getBlockState(new BlockPos(x, y, z)).isAir(),

    filNormal = (world, x, y, z, block) -> {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = world.getBlockState(pos);
        return !state.getCollisionShape(world, pos).isEmpty();
    },

    filEverything = (world, x, y, z, block) -> false,

    filReplacable = (world, x, y, z, block) -> !world.getBlockState(new BlockPos(x, y, z)).canBeReplaced();

}
