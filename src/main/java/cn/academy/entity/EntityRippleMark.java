package cn.academy.entity;
import cn.academy.ACEntities;
import cn.academy.client.render.entity.RippleMarkRender;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.entityx.EntityAdvanced;
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
public class EntityRippleMark extends EntityAdvanced
{

    public final Color color = Colors.white();
    public final double creationTime = GameTimer.getTime();

    public EntityRippleMark(Level world) {
        super(ACEntities.RIPPLE_MARK, world);
    }
    
    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 1;
    }
    
    @Override
    protected void readEntityFromNBT(CompoundTag tag) {}

    @Override
    protected void writeEntityToNBT(CompoundTag tag) {}

}