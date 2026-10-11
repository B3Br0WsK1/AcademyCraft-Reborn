package cn.academy.ability;
import cn.academy.AcademyCraft;
import cn.academy.event.BlockDestroyEvent;
import cn.lambdalib2.registry.StateEventCallback;
import net.minecraft.core.BlockPos;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.event.SubscribeEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import cn.lambdalib2.event.EventHub;
import cn.lambdalib2.util.Configuration;
import cn.lambdalib2.util.Configuration.Property;
import org.apache.commons.lang3.ArrayUtils;


/**
 * INTERNAL CLASS
 */
public class AbilityPipeline {

    private AbilityPipeline() {}

    /**
     * @return Whether we can break any block at all
     */
    static boolean canBreakBlock(Level world) {
        String dimId = world.dimension().identifier().toString();
        return propDestroyBlocks.getBoolean() ||
                ArrayUtils.contains(propWorldsDestroyingBlocks.getStringList(), dimId) ||
                ArrayUtils.contains(propWorldsDestroyingBlocks.getStringList(), world.dimension().identifier().getPath());
    }

    static boolean isAllWorldDisableBreakBlock() {
        return !propDestroyBlocks.getBoolean() && propWorldsDestroyingBlocks.getIntList().length == 0;
    }

    /**
     * @return Whether PvP is enabled.
     */
    static boolean canAttackPlayer() {
        return propAttackPlayer.getBoolean();
    }

    /**
     * Tests if the block at the specified coordinates can be broken by a
     * certain player.
     * @return Whether the block can be really broken.
     */
    static boolean canBreakBlock(Level world, Player player,
        int x, int y, int z)
    {
        return !EventHub.BUS.post(
            new BlockDestroyEvent(world, player, new BlockPos(x, y, z)));
    }
    static boolean canBreakBlock(Level world, Player player, BlockPos pos)
    {
        return !EventHub.BUS.post(
                new BlockDestroyEvent(world, player, pos));
    }

    public static boolean canUseMouseWheel(){
        return propUseMouseWheel.getBoolean();
    }

    /**
     * Tests if the block at the specified coordinates can be broken.
     * @return Whether the block can be really broken.
     */
    static boolean canBreakBlock(Level world, int x, int y, int z) {
        return !EventHub.BUS.post(
            new BlockDestroyEvent(world, new BlockPos(x, y, z)));
    }
    static boolean canBreakBlock(Level world, BlockPos pos) {
        return !EventHub.BUS.post(
                new BlockDestroyEvent(world, pos));
    }

    /**
     * Tests if the block at the specified coordinates can be broken by a
     * certain player.
     * @return Whether the block can be really broken.
     */
    static boolean canBreakBlock(Player player, int x, int y, int z) {
        return !EventHub.BUS.post(
            new BlockDestroyEvent(player, new BlockPos(x, y, z)));
    }
    static boolean canBreakBlock(Player player, BlockPos pos) {
        return !EventHub.BUS.post(
                new BlockDestroyEvent(player, pos));
    }

    // PROPERTIES
    private static Property propAttackPlayer;
    private static Property propDestroyBlocks;
    private static Property propWorldsDestroyingBlocks;
    private static Property propUseMouseWheel;

    @StateEventCallback
    private static void _init(StateEvents.Init event) {
        Configuration conf = AcademyCraft.config;

        propAttackPlayer = conf.get("generic", "attackPlayer", true, "Whether the skills are effective on players.");
        propDestroyBlocks = conf.get("generic", "destroyBlocks", true, "Whether the skills will destroy blocks in the world.");
        propWorldsDestroyingBlocks = conf.get("generic", "worldsWhitelistedDestroyingBlocks", new String[]{},
                "The worlds which whitelisted destroying blocks. Dimension ids (e.g. minecraft:the_nether) and dimension names are supported.");
        propUseMouseWheel = conf.get("generic","useMouseWheel",false,"Whether teleporter can use mouse wheel to control the destination.");
        EventHub.BUS.register(new AbilityPipeline());
    }

    @SubscribeEvent
    public void onBlockDestroy(BlockDestroyEvent event) {
        if(!canBreakBlock(event.world))
            event.setCanceled(true);
    }

}