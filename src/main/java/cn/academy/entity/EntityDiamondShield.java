package cn.academy.entity;
import cn.academy.ACEntities;
import cn.academy.client.render.entity.RenderDiamondShield;
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
public class EntityDiamondShield extends EntityAdvanced
{

    public static final float SIZE = 1.8f;
    
    final Player player;

    public EntityDiamondShield(Player _player) {
        super(ACEntities.DIAMOND_SHIELD, _player.level());
        player = _player;
        setPosition(player.getX(), player.getY(), player.getZ());
        updatePos();
    }
    
    @Override
    public void onUpdate() {
        super.onUpdate();
        updatePos();
    }

    private void updatePos() {
        Vec3 pos =
            VecUtils.add(player.position().add(0, 1.1, 0), VecUtils.multiply(player.getLookAngle(), 1));

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