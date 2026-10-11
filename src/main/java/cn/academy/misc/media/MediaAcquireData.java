package cn.academy.misc.media;

import cn.lambdalib2.datapart.DataPart;
import cn.lambdalib2.datapart.EntityData;
import cn.lambdalib2.datapart.RegDataPart;
import cn.lambdalib2.s11n.SerializeIncluded;
import cn.lambdalib2.util.Side;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores which internal medias the player has acquired.
 */
@RegDataPart(Player.class)
public class MediaAcquireData extends DataPart<Player> {

    public static MediaAcquireData get(Player player) {
        return EntityData.get(player).getPart(MediaAcquireData.class);
    }

    /**
     * Bitmask of acquired internal medias.
     */
    @SerializeIncluded
    private long bits = 0;

    public MediaAcquireData() {
        setClientNeedSync();
        setNBTStorage();
    }

    /**
     * Install the given media. It must be internal. Should only be called in SERVER.
     */
    public void install(Media media) {
        if (media.external)
            throw new IllegalArgumentException("Can't install external media");
        checkSide(Side.SERVER);

        int idx = MediaManager.internalMedias().indexOf(media);
        if (idx >= 0 && idx < 64) {
            bits |= 1L << idx;
        }

        sync();
    }

    /**
     * @return All installed **internal** medias.
     */
    public List<Media> installed() {
        List<Media> ret = new ArrayList<>();
        List<Media> internal = MediaManager.internalMedias();
        for (int i = 0; i < internal.size() && i < 64; ++i) {
            if ((bits & (1L << i)) != 0) {
                ret.add(internal.get(i));
            }
        }
        return ret;
    }

    /**
     * @return Whether a media is installed for player. True for all external media.
     */
    public boolean isInstalled(Media media) {
        if (media.external) return true;
        int idx = MediaManager.internalMedias().indexOf(media);
        return idx >= 0 && idx < 64 && (bits & (1L << idx)) != 0;
    }

    @Override
    public void toNBT(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Media media : installed()) {
            list.add(StringTag.valueOf(media.id));
        }
        tag.put("acquired", list);
    }

    @Override
    public void fromNBT(CompoundTag tag) {
        bits = 0;
        List<Media> internal = MediaManager.internalMedias();
        for (Tag t : tag.getListOrEmpty("acquired")) {
            t.asString().ifPresent(id -> {
                int idx = internal.indexOf(MediaManager.get(id));
                if (idx >= 0 && idx < 64) {
                    bits |= 1L << idx;
                }
            });
        }
    }
}
