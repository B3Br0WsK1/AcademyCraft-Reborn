/**
 * Copyright (c) Lambda Innovation, 2013-2016
 * This file is part of LambdaLib modding library.
 * https://github.com/LambdaInnovation/LambdaLib
 * Licensed under MIT, see project root for more information.
 */
package cn.lambdalib2.util;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public interface IBlockSelector {

    boolean accepts(Level world, int x, int y, int z, Block block);

}
