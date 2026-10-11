package cn.academy.entity;
import cn.academy.ACEntities;
import cn.lambdalib2.util.VecUtils;
import cn.lambdalib2.util.entityx.EntityAdvanced;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;


/**
 * @author WeAthFolD
 */
@Environment(EnvType.CLIENT)
public class EntityMdShield extends EntityAdvanced
{

    public static final float SIZE = 1.8f;
    
    // Intrusive render states
    public float rotation;
    public double lastRender;
    
    final Player player;

    public EntityMdShield(Player _player) {
        super(ACEntities.MD_SHIELD, _player.level());
        player = _player;
        updatePos();
    }
    
    @Override
    public void onUpdate() {
        super.onUpdate();
        updatePos();
    }

    private void updatePos() {
        Vec3 pos = VecUtils.add(
            VecUtils.add(player.position(), VecUtils.multiply(player.getLookAngle(), 1)),
            new Vec3(0,1.1,0)
        );
        this.setPos(pos.x, this.getY(), this.getZ());
        this.setPos(this.getX(), pos.y, this.getZ());
        this.setPos(this.getX(), this.getY(), pos.z);
        this.setYRot(player.yHeadRot);
        this.setXRot(player.getXRot());
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