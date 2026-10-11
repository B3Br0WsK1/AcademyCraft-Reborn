/**
 * Copyright (c) Lambda Innovation, 2013-2016
 * This file is part of LambdaLib modding library.
 * https://github.com/LambdaInnovation/LambdaLib
 * Licensed under MIT, see project root for more information.
 */
package cn.lambdalib2.datapart;

import cn.lambdalib2.s11n.nbt.NBTS11n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * A data tag for storage-only, lightweight stuffs
 * @author WeAthFolD
 */
@RegDataPart(Player.class)
public class PlayerDataTag extends DataPart<Player> {

    public static PlayerDataTag get(Player player) {
        return EntityData.get(player).getPart(PlayerDataTag.class);
    }

    private CompoundTag theTag = new CompoundTag();

    {
        setNBTStorage();
    }

    @Override
    public void fromNBT(CompoundTag tag) {
        theTag = tag;
    }

    @Override
    public void toNBT(CompoundTag tag) {
        NBTS11n.write(tag, theTag);
    }

    public CompoundTag getTag() {
        if(theTag == null)
            theTag = new CompoundTag();
        return theTag;
    }

}
