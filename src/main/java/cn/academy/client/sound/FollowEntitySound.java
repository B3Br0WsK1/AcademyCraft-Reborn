package cn.academy.client.sound;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

/**
 * A stoppable&repeatable sound that follows an entity.
 * @author WeAthFolD
 */
@Environment(EnvType.CLIENT)
public class FollowEntitySound extends PositionedSound {

    public final Entity entity;

    public FollowEntitySound(Entity _entity, String name, SoundSource category) {
        super(_entity.getX(), _entity.getY(), _entity.getZ(), name, category);
        entity = _entity;
    }

    @Override
    public FollowEntitySound setVolume(float volume) {
        super.setVolume(volume);
        return this;
    }

    @Override
    public FollowEntitySound setLoop() {
        super.setLoop();
        return this;
    }

    @Override
    public void update() {
        setPos(entity.getX(), entity.getY(), entity.getZ());
    }

}
