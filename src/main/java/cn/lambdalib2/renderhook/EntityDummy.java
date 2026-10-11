package cn.lambdalib2.renderhook;

import cn.lambdalib2.util.ViewOptimize.IAssociatePlayer;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A client-local entity following a player, used to render {@link PlayerRenderHook}s.
 * @author WeAthFolD
 */
public class EntityDummy extends Entity implements IAssociatePlayer {

    Player player;
    Object data;

    boolean set;
    float lastRotationYaw, lastRotationYawHead, rotationYawHead;
    float lastRotationPitch;

    public EntityDummy(EntityType<? extends EntityDummy> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    void init(Object _data) {
        data = _data;
        player = ((DummyRenderData) _data).getEntity();
        setPos(player.getX(), player.getY(), player.getZ());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public void tick() {
        if (player == null) {
            discard();
            return;
        }
        if(!set) {
            set = true;
            lastRotationYaw = player.yBodyRot;
            lastRotationYawHead = player.getYHeadRot();
            lastRotationPitch = player.getXRot();
        } else {
            lastRotationYaw = getYRot();
            lastRotationYawHead = rotationYawHead;
            lastRotationPitch = getXRot();
        }

        xo = getX();
        yo = getY();
        zo = getZ();
        setPos(player.getX(), player.getY(), player.getZ());

        setYRot(player.yBodyRot);
        rotationYawHead = player.getYHeadRot();
        setXRot(player.getXRot());
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}

}
