package cn.academy.misc.media;

import cn.academy.AcademyCraft;
import cn.lambdalib2.datapart.DataPart;
import cn.lambdalib2.datapart.EntityData;
import cn.lambdalib2.datapart.RegDataPart;
import cn.lambdalib2.util.Configuration.Property;
import cn.lambdalib2.util.Side;
import cn.lambdalib2.util.TickScheduler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.openal.AL10;
import org.lwjgl.system.MemoryUtil;

import javax.sound.sampled.AudioFormat;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Plays medias. The 1.12 version used the paulscode sound system; this version streams the Ogg file into its own
 *  OpenAL source (the OpenAL context of Minecraft's sound engine is used).
 */
@Environment(EnvType.CLIENT)
@RegDataPart(value=Player.class, side=Side.CLIENT)
public class MediaBackend extends DataPart<Player> {

    public record PlayInfo(Media media, boolean paused, float time) {
        public String displayTime() {
            return MediaManager.getDisplayTime(time);
        }
    }

    public static MediaBackend get(Player player) {
        return EntityData.get(player).getPart(MediaBackend.class);
    }

    public static MediaBackend get() {
        return get(Minecraft.getInstance().player);
    }

    private static final int BUFFER_COUNT = 4;

    private static Property volumeProperty;

    private static Property volumeProperty() {
        if (volumeProperty == null) {
            volumeProperty = AcademyCraft.config.get("media_player", "volume", 1.0, "Media Player's volume");
        }
        return volumeProperty;
    }

    private Stream stream;
    private Media lastPlay;

    private final TickScheduler scheduler = new TickScheduler();

    public MediaBackend() {
        setTick(true);

        scheduler.every(5).run(() -> {
            PlayInfo info = currentPlaying();
            if (info != null && !info.paused()) {
                Minecraft.getInstance().getMusicManager().stopPlaying();
            }
        });
    }

    public void play(Media media) {
        stopCurrent();
        if (media.source == null) return;
        try {
            stream = new Stream(media);
            stream.setVolume(getVolume());
            stream.play();
            lastPlay = media;
        } catch (Exception e) {
            AcademyCraft.log.error("Can't play media " + media.id, e);
            if (stream != null) stream.close();
            stream = null;
        }
    }

    public void pauseCurrent() {
        if (stream != null) stream.pause();
    }

    public void continueCurrent() {
        if (stream != null) stream.play();
    }

    public void stopCurrent() {
        if (stream != null) {
            stream.close();
            stream = null;
        }
    }

    /**
     * @return The current play info, or null if nothing is playing.
     */
    public PlayInfo currentPlaying() {
        if (stream == null) return null;
        return new PlayInfo(stream.media, stream.paused, stream.getTime());
    }

    public Media lastPlayed() {
        return lastPlay;
    }

    public float getVolume() {
        return (float) volumeProperty().getDouble();
    }

    public void setVolume(float value) {
        volumeProperty().set(String.valueOf(value));
        if (stream != null) stream.setVolume(value);
    }

    @Override
    public void tick() {
        scheduler.runTick();
        if (stream != null && !stream.update()) {
            stream.close();
            stream = null;
        }
    }

    /**
     * An OpenAL streaming source.
     */
    private static final class Stream {
        final Media media;
        final JOrbisAudioStream audio;
        final int source;
        final int[] buffers = new int[BUFFER_COUNT];
        final int format;
        final int sampleRate;
        final int frameSize;
        final int chunkSize;
        final java.util.Map<Integer, Integer> bufferFrames = new java.util.HashMap<>();
        long playedFrames = 0;
        boolean eof = false;
        boolean paused = false;

        Stream(Media media) throws IOException {
            this.media = media;
            InputStream is = new BufferedInputStream(media.source.openStream());
            audio = new JOrbisAudioStream(is);
            AudioFormat fmt = audio.getFormat();
            int channels = fmt.getChannels();
            sampleRate = (int) fmt.getSampleRate();
            frameSize = channels * 2;
            format = channels == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
            chunkSize = sampleRate * frameSize; // ~1 second per buffer

            source = AL10.alGenSources();
            AL10.alSourcei(source, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
            AL10.alSource3f(source, AL10.AL_POSITION, 0, 0, 0);
            AL10.alSourcef(source, AL10.AL_ROLLOFF_FACTOR, 0);
            for (int i = 0; i < BUFFER_COUNT; ++i) {
                buffers[i] = AL10.alGenBuffers();
                if (!fill(buffers[i])) break;
                AL10.alSourceQueueBuffers(source, buffers[i]);
            }
        }

        private boolean fill(int buffer) {
            if (eof) return false;
            try {
                ByteBuffer data = audio.read(chunkSize);
                if (data == null || data.remaining() == 0) {
                    eof = true;
                    return false;
                }
                AL10.alBufferData(buffer, format, data, sampleRate);
                bufferFrames.put(buffer, data.remaining() / frameSize);
                return true;
            } catch (IOException e) {
                eof = true;
                return false;
            }
        }

        void play() {
            paused = false;
            AL10.alSourcePlay(source);
        }

        void pause() {
            paused = true;
            AL10.alSourcePause(source);
        }

        void setVolume(float v) {
            AL10.alSourcef(source, AL10.AL_GAIN, v);
        }

        float getTime() {
            int offset = AL10.alGetSourcei(source, 0x1025); // AL_SAMPLE_OFFSET
            return (playedFrames + offset) / (float) sampleRate;
        }

        /**
         * @return Whether the stream is still alive.
         */
        boolean update() {
            int processed = AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED);
            while (processed-- > 0) {
                int buf = AL10.alSourceUnqueueBuffers(source);
                playedFrames += bufferFrames.getOrDefault(buf, 0);
                if (fill(buf)) {
                    AL10.alSourceQueueBuffers(source, buf);
                }
            }
            int queued = AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED);
            if (queued == 0 && eof) {
                return false;
            }
            int state = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
            if (!paused && state != AL10.AL_PLAYING && queued > 0) {
                AL10.alSourcePlay(source); // Recover from buffer underrun
            }
            return true;
        }

        void close() {
            AL10.alSourceStop(source);
            int queued = AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED);
            while (queued-- > 0) {
                AL10.alSourceUnqueueBuffers(source);
            }
            AL10.alDeleteSources(source);
            for (int b : buffers) {
                if (b != 0) AL10.alDeleteBuffers(b);
            }
            try {
                audio.close();
            } catch (IOException ignored) {}
        }
    }

}
