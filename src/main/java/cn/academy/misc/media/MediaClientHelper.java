package cn.academy.misc.media;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Client helpers of the media system.
 */
@Environment(EnvType.CLIENT)
final class MediaClientHelper {

    private MediaClientHelper() {}

    static void loadCover(Media media, File coverFile) throws IOException {
        try (InputStream is = new FileInputStream(coverFile)) {
            NativeImage image = NativeImage.read(is);
            Identifier id = Identifier.fromNamespaceAndPath("academy", "media/cover_external/" +
                media.id.toLowerCase().replaceAll("[^a-z0-9_.-]", "_"));
            Minecraft.getInstance().execute(() ->
                Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "ac_media_cover", image)));
            media.cover = id;
        }
    }

}
