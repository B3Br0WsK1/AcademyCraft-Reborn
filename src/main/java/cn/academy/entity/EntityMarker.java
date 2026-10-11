package cn.academy.entity;
import cn.academy.ACEntities;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.entityx.EntityAdvanced;
import net.minecraft.world.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import cn.lambdalib2.util.Color;
import net.fabricmc.api.EnvType;


/**
 * @author WeAthFolD
 */
@Environment(EnvType.CLIENT)
public class EntityMarker extends EntityAdvanced
{

    public Entity target = null;
    public Color color = Colors.white();
    public boolean ignoreDepth = false;
    /** Size of the marker when it has no target. */
    public float width = 0.5f, height = 0.5f;

    public EntityMarker(Entity entity) {
        this(entity.level());
        setPosition(entity.getX(), entity.getY(), entity.getZ());
        target = entity;
    }

    public EntityMarker(Level world) {
        super(ACEntities.MARKER, world);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (target != null)
            setPosition(target.getX(), target.getY(), target.getZ());
    }

    @Override
    protected void readEntityFromNBT(CompoundTag tag) {
        setDead();
    }

    @Override
    protected void writeEntityToNBT(CompoundTag tag) {
    }

}