package cn.academy.item;

import net.minecraft.world.item.Item;

/**
 * Matrix core. The 1.12 metadata levels (0..2) are separate items in this port.
 * @author WeAthFolD
 */
public class ItemMatrixCore extends Item {

    public final int level;

    public ItemMatrixCore(int level, Properties props) {
        super(props);
        this.level = level;
    }

}
