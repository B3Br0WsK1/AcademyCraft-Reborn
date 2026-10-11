package cn.academy.item;

import cn.academy.misc.media.Media;
import cn.academy.misc.media.MediaAcquireData;
import cn.academy.misc.media.MediaApp;
import cn.academy.misc.media.MediaManager;
import cn.academy.terminal.TerminalData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Consumer;

/**
 * A disc that unlocks a media (music) in the media player app. The media index is stored in {@link ItemMeta}.
 */
public class MediaItem extends Item implements ISubItemProvider
{
    public MediaItem(Properties props)
    {
        super(props);
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        if (!world.isClientSide())
        {
            Media media = getMedia(stack);
            if (media == null)
                return InteractionResult.PASS;

            MediaAcquireData acquireData = MediaAcquireData.get(player);
            TerminalData tData = TerminalData.get(player);

            if (!tData.isInstalled(MediaApp.INSTANCE))
            {
                player.sendSystemMessage(Component.translatable("ac.media.notinstalled"));
            }
            else if (acquireData.isInstalled(media))
            {
                player.sendSystemMessage(Component.translatable("ac.media.haveone", media.name()));
            }
            else
            {
                acquireData.install(media);
                if (!player.getAbilities().instabuild)
                {
                    stack.shrink(1);
                }
                player.sendSystemMessage(Component.translatable("ac.media.acquired", media.name()));
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public Component getName(ItemStack stack)
    {
        Media media = getMedia(stack);
        return media == null ? super.getName(stack) : Component.literal(media.name());
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> builder, TooltipFlag flag)
    {
        Media media = getMedia(stack);
        if (media != null) {
            builder.accept(Component.literal(media.desc()));
        }
    }

    @Override
    public void addSubItems(Consumer<ItemStack> items)
    {
        List<Media> medias = MediaManager.internalMedias();
        for(int i=0;i<medias.size();i++)
        {
            items.accept(ItemMeta.of(this, 1, i));
        }
    }

    private Media getMedia(ItemStack stack)
    {
        List<Media> all = MediaManager.allMedias();
        int id = ItemMeta.get(stack);
        return id >= 0 && id < all.size() ? all.get(id) : null;
    }
}
