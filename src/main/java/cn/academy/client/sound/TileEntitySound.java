package cn.academy.client.sound;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Plays sound on a certain BlockEntity. Automatically stops when it is invalid.
 */
@Environment(EnvType.CLIENT)
public class TileEntitySound extends PositionedSound {

    private final BlockEntity te;

    public TileEntitySound(BlockEntity _te, String name) {
        super(_te.getBlockPos().getX() +.5, _te.getBlockPos().getY() + .5, _te.getBlockPos().getZ() + .5,
            name, SoundSource.BLOCKS);
        te = _te;
    }

    @Override
    public void update() {
        super.update();

        if (te.isRemoved()) {
            stop();
        }
    }
}
