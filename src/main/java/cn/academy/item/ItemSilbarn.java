package cn.academy.item;

import cn.academy.entity.EntitySilbarn;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Throwable item spawning {@link EntitySilbarn}. Rendered with its OBJ model (see the item model definition).
 * @author WeathFolD
 */
public class ItemSilbarn extends Item {

    public ItemSilbarn(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        world.playSound(
            null,
            player.getX(), player.getY(), player.getZ(),
            SoundEvents.EGG_THROW,
            SoundSource.PLAYERS,
            0.5F,
            0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F)
        );
        if(!world.isClientSide()) {
            world.addFreshEntity(new EntitySilbarn(player));
        }
        if(!player.getAbilities().instabuild)
            stack.shrink(1);
        return InteractionResult.SUCCESS;
    }

}
