package cn.academy.entity;
import cn.academy.ACEntities;
import cn.academy.datapart.AbilityData;
import cn.academy.client.render.entity.MarkRender;
import cn.academy.client.render.misc.TPParticleFactory;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.entityx.EntityAdvanced;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;


/**
 * Spawn a position mark indicating where the player would be teleport to. You
 * should spawn it in CLIENT ONLY.
 * 
 * @author WeathFolD
 */
@Environment(EnvType.CLIENT)
public class EntityTPMarking extends EntityAdvanced {

//    static TPParticleFactory particleFac = TPParticleFactory.instance;

    final AbilityData data;
    protected final Player player;

    public boolean available = true;

    public EntityTPMarking(Player player) {
        super(ACEntities.TP_MARKING, player.level());
        data = AbilityData.get(player);
        this.player = player;
        setPosition(player.getX(), player.getY(), player.getZ());
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        setXRot(player.getXRot());
        setYRot(player.getYRot());

        if (available && rand.nextDouble() < 0.4) {
            TPParticleFactory.instance.setPosition(getX() + RandUtils.ranged(-1, 1), getY() + RandUtils.ranged(0.2, 1.6) - 1.6,
                    getZ() + RandUtils.ranged(-1, 1));
            TPParticleFactory.instance.setVelocity(RandUtils.ranged(-.03, .03), RandUtils.ranged(0, 0.05),
                    RandUtils.ranged(-.03, .03));

            world.addFreshEntity(TPParticleFactory.instance.next(world));
        }
    }

    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 1;
    }

    public double getDist() {
        return this.distanceToSqr(player);
    }

    @Override
    protected void readEntityFromNBT(CompoundTag p_70037_1_) {
    }

    @Override
    protected void writeEntityToNBT(CompoundTag p_70014_1_) {
    }

}