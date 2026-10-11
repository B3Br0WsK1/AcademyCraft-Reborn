package cn.academy.misc.media;

import cn.academy.AcademyCraft;
import cn.lambdalib2.util.Configuration.Property;
import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;

import java.net.URL;

/**
 * A playable media (music). Internal medias are shipped with the mod and must be acquired with a media item;
 *  external medias are placed by the user in the .minecraft/acmedia/source folder.
 */
public class Media {

    public final boolean external;
    public final String id;
    public final URL source;
    public Identifier cover;
    public final float lengthSecs;

    public Media(boolean external, String id, URL source, float lengthSecs) {
        this.external = external;
        this.id = id;
        this.source = source;
        this.cover = Identifier.fromNamespaceAndPath("academy", "media/cover/" + id + ".png");
        this.lengthSecs = lengthSecs;
    }

    public Property propExternalName() {
        return AcademyCraft.config.get("media", id + "_name", id);
    }

    public Property propExternalDesc() {
        return AcademyCraft.config.get("media", id + "_desc", id);
    }

    public String name() {
        if (external) {
            return propExternalName().getString();
        }
        return Language.getInstance().getOrDefault("ac.media." + id + ".name");
    }

    public String desc() {
        if (external) {
            return propExternalDesc().getString();
        }
        return Language.getInstance().getOrDefault("ac.media." + id + ".desc");
    }

    public String displayLength() {
        return MediaManager.getDisplayTime(lengthSecs);
    }

}
