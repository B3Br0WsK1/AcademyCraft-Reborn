package cn.academy.misc.media;

import cn.academy.AcademyCraft;
import cn.academy.Resources;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.util.ResourceUtils;
import com.jcraft.jorbis.VorbisFile;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of the medias.
 */
public final class MediaManager {

    private MediaManager() {}

    private static final Map<String, Media> medias = new HashMap<>();
    private static final List<Media> internalMediasList = new ArrayList<>();
    private static final List<Media> mediasList = new ArrayList<>();

    public static final Identifier missingCover = Resources.getTexture("guis/icons/icon_nomedia");

    public static void register(Media media) {
        medias.put(media.id, media);
        mediasList.add(media);
        if (!media.external) {
            internalMediasList.add(media);
        }
    }

    public static Media get(String id) {
        return medias.get(id);
    }

    public static List<Media> internalMedias() {
        return Collections.unmodifiableList(internalMediasList);
    }

    public static int internalMediaCount() {
        if (internalMediasList.isEmpty()) {
            // The loot table can be built before init (e.g. on the dedicated server it's fine as init runs first)
            return readDefaultConfig().getStringList("default_medias").size();
        }
        return internalMediasList.size();
    }

    public static List<Media> allMedias() {
        return Collections.unmodifiableList(mediasList);
    }

    public static String getDisplayTime(float secs) {
        int totalSecs = (int) secs;
        int minutes = totalSecs / 60;
        int seconds = totalSecs % 60;
        return wrapTime(minutes) + ":" + wrapTime(seconds);
    }

    private static String wrapTime(int x) {
        return x < 10 ? "0" + x : Integer.toString(x);
    }

    // ---- Init

    @StateEventCallback
    private static void init(StateEvents.Init ev) {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            loadClient();
        } else {
            loadServer();
        }
    }

    @Environment(EnvType.CLIENT)
    private static void loadClient() {
        Config conf = readDefaultConfig();
        File path = checkPath(rootFolder());

        // Copy the readme_template.txt to the folder.
        try (InputStream is = ResourceUtils.getResourceStream(Resources.res("media/readme_template.txt"))) {
            Files.copy(is, new File(path, "README.txt").toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            AcademyCraft.log.error("Can't copy media readme file.");
        }

        // Parse default medias.
        for (String id : conf.getStringList("default_medias")) {
            try (InputStream is = ResourceUtils.getResourceStream(Resources.res("media/source/" + id + ".ogg"))) {
                File dst = new File(path, id + ".ogg");
                Files.copy(is, dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
                URL url = dst.toURI().toURL();
                register(new Media(false, id, url, calculateLength(dst)));
            } catch (Exception e) {
                AcademyCraft.log.error("Can't copy media file " + id + ".");
            }
        }

        // Parse external medias.
        File coverPath = checkPath(new File(path, "cover"));
        File sourcePath = checkPath(new File(path, "source"));

        File[] files = sourcePath.listFiles();
        if (files != null) {
            for (File sound : files) {
                if (!sound.getName().endsWith(".ogg")) continue;
                String id = sound.getName().substring(0, sound.getName().indexOf('.'));
                float length = calculateLength(sound);
                if (length < 0) continue;
                try {
                    Media media = new Media(true, id, sound.toURI().toURL(), length);
                    File coverFile = new File(coverPath, id + ".png");
                    if (coverFile.isFile()) {
                        MediaClientHelper.loadCover(media, coverFile);
                    } else {
                        media.cover = missingCover;
                    }
                    register(media);
                } catch (IOException e) {
                    AcademyCraft.log.error("Can't load external media " + id, e);
                }
            }
        }
    }

    private static void loadServer() {
        Config conf = readDefaultConfig();
        for (String id : conf.getStringList("default_medias")) {
            register(new Media(false, id, null, 0));
        }
    }

    private static File checkPath(File file) {
        if (!file.exists() && !file.mkdirs()) {
            AcademyCraft.log.error("Can't create media folder " + file);
        }
        return file;
    }

    /**
     * @return Length in seconds, or -1 if the file can't be read.
     */
    private static float calculateLength(File file) {
        try {
            String p = URLDecoder.decode(file.getAbsolutePath(), StandardCharsets.UTF_8);
            VorbisFile vf = new VorbisFile(p);
            float ret = vf.time_total(-1);
            vf.close();
            return ret;
        } catch (Exception e) {
            return -1;
        }
    }

    private static File rootFolder() {
        return new File(FabricLoader.getInstance().getGameDir().toFile(), "acmedia");
    }

    static Config readDefaultConfig() {
        try (InputStream is = ResourceUtils.getResourceStream(Resources.res("media/default.conf"))) {
            return ConfigFactory.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
