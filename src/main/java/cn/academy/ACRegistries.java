package cn.academy;

import cn.academy.block.fluid.ACFluids;
import cn.academy.item.ItemMeta;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;

/**
 * Triggers the registration of game objects in the correct order, before anything else runs in PreInit.
 */
final class ACRegistries {

    private ACRegistries() {}

    @StateEventCallback(priority = 1000)
    private static void preInit(StateEvents.PreInit ev) {
        ItemMeta.init();
        ACSoundEvents.init();
        ACFluids.init();
        ACBlocks.init();
        ACTiles.init();
        ACItems.init();
        ACEntities.init();
    }

}
