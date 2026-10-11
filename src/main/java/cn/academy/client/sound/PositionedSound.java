package cn.academy.client.sound;

import cn.academy.Resources;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * A stoppable & repeatable sound at a (changeable) position.
 * @author WeAthFolD
 */
@Environment(EnvType.CLIENT)
public class PositionedSound extends AbstractSoundInstance implements TickableSoundInstance {

    private boolean stopped;

    public PositionedSound(double _x, double _y, double _z, String name, SoundSource category) {
        super(Resources.sound(name), category, RandomSource.create());
        x = _x;
        y = _y;
        z = _z;
    }

    public PositionedSound setVolume(float volume) {
        this.volume = volume;
        return this;
    }

    public PositionedSound setPitch(float pitch) {
        this.pitch = pitch;
        return this;
    }

    public PositionedSound setLoop() {
        this.looping = true;
        return this;
    }

    public void setPos(double _x, double _y, double _z) {
        x = _x;
        y = _y;
        z = _z;
    }

    public void stop() {
        this.stopped = true;
        this.looping = false;
    }

    @Override
    public boolean isStopped() {
        return stopped;
    }

    @Override
    public void tick() {
        update();
    }

    /**
     * Called each tick while playing.
     */
    public void update() {}

}
