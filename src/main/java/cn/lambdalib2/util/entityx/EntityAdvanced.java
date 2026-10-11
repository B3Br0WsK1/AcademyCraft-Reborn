package cn.lambdalib2.util.entityx;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * An entity driven by {@link EntityX}: motion handlers, delayed callbacks and entity events.
 */
public abstract class EntityAdvanced extends Entity {

    public EntityX entityX;

    /**
     * 1.12 compatibility: the world of the entity (entities can't change their level in 26.x).
     */
    protected final Level world;

    /**
     * 1.12 compatibility alias of {@link #random}.
     */
    protected final RandomSource rand;

    public EntityAdvanced(EntityType<?> type, Level world) {
        super(type, world);
        this.world = world;
        this.rand = this.random;
        entityX = new EntityX(this);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        readEntityFromNBT(input.read("ll_data", CompoundTag.CODEC).orElseGet(CompoundTag::new));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        CompoundTag tag = new CompoundTag();
        writeEntityToNBT(tag);
        output.store("ll_data", CompoundTag.CODEC, tag);
    }

    /**
     * 1.12 style save data reading.
     */
    protected void readEntityFromNBT(CompoundTag tag) {}

    /**
     * 1.12 style save data writing.
     */
    protected void writeEntityToNBT(CompoundTag tag) {}

    @Override
    public boolean shouldRenderAtSqrDistance(double distSq) {
        return distSq < 256 * 256;
    }

    // ---- 1.12 compatibility helpers

    public void setDead() {
        discard();
    }

    public Level getEntityWorld() {
        return level();
    }

    public void setPosition(double x, double y, double z) {
        setPos(x, y, z);
    }

    public Vec3 getPositionVector() {
        return position();
    }

    /**
     * No render passes in 26.x; kept so that old overrides compile.
     */
    public boolean shouldRenderInPass(int pass) {
        return true;
    }

    private boolean firstUpdate = true;

    @Override
    public final void tick() {
        onUpdate();
    }

    /**
     * The entity update (1.12 name). Overriders must call super.
     */
    public void onUpdate() {
        if(firstUpdate) {
            firstUpdate = false;
            entityX.startUpdate();
            onFirstUpdate();
        }
        entityX.update();
    }

    public boolean firstUpdated() {
        return !firstUpdate;
    }

    protected void onFirstUpdate() {

    }

    /**
     * Designed for prototype pattern.
     */
    public void reset() {
        firstUpdate = true;
    }

    public void addMotionHandler(MotionHandler mh) {
        entityX.addMotionHandler(mh);
    }

    public void removeMotionHandlers() {
        entityX.removeMotionHandlers();
    }

    @SuppressWarnings("unchecked")
    public <U extends MotionHandler> U getMotionHandler(Class<? extends U> klass) {
        return (U) entityX.getMotionHandler(klass);
    }

    public void execute(EntityCallback c) {
        entityX.execute(c);
    }

    public void executeAfter(EntityCallback c, int ticks) {
        entityX.executeAfter(c, ticks);
    }

    public void postEvent(EntityEvent event) {
        entityX.postEvent(event);
    }

    public void regEventHandler(EntityEventHandler eeh) {
        entityX.regEventHandler(eeh);
    }

    public void resetEntityX() {
        entityX = new EntityX(this);
    }

}
