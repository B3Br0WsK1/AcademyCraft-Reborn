package cn.academy.item;

import cn.academy.Resources;
import cn.academy.entity.EntityCoinThrowing;
import cn.academy.event.CoinThrowEvent;
import cn.lambdalib2.event.EventHub;
import cn.lambdalib2.event.SubscribeEvent;
import cn.lambdalib2.event.mc.TickEvent.PlayerTickEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * The coin, used by Railgun.
 * @author KSkun
 */
public class ItemCoin extends Item {

    // Key: PlayerName
    private static Map<String, EntityCoinThrowing> client = new HashMap<>(), server = new HashMap<>();

    public ItemCoin(Properties props) {
        super(props);
        EventHub.BUS.register(this);
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent event) {
        Player player = event.player;
        Map<String, EntityCoinThrowing> map = getMap(player);
        EntityCoinThrowing etc = getPlayerCoin(player);
        if(etc != null) {
            if(etc.isRemoved() || etc.level() != player.level()) {
                map.remove(player.getName().getString());
            }
        }
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if(getPlayerCoin(player) != null) {
            return InteractionResult.PASS;
        }

        //Spawn at both side, not syncing for render effect purpose
        EntityCoinThrowing etc = new EntityCoinThrowing(player, stack);
        world.addFreshEntity(etc);

        player.playSound(Resources.sound("entity.flipcoin"), 0.5f, 1.0f);
        setPlayerCoin(player, etc);

        EventHub.BUS.post(new CoinThrowEvent(player, etc));
        if(!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    public static EntityCoinThrowing getPlayerCoin(Player player) {
        EntityCoinThrowing etc = getMap(player).get(player.getName().getString());
        if(etc != null && !etc.isRemoved())
            return etc;
        return null;
    }

    public static void setPlayerCoin(Player player, EntityCoinThrowing etc) {
        Map<String, EntityCoinThrowing> map = getMap(player);
        map.put(player.getName().getString(), etc);
    }

    private static Map<String, EntityCoinThrowing> getMap(Player player) {
        return player.level().isClientSide() ? client : server;
    }

}
