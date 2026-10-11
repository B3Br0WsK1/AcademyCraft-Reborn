package cn.academy.entity;

import cn.academy.ACEntities;
import cn.academy.ACItems;
import cn.academy.Resources;
import cn.lambdalib2.util.EntitySelectors;
import cn.lambdalib2.util.RayTraceResult;
import cn.lambdalib2.util.entityx.EntityAdvanced;
import cn.lambdalib2.util.entityx.MotionHandler;
import cn.lambdalib2.util.entityx.event.CollideEvent;
import cn.lambdalib2.util.entityx.handlers.Rigidbody;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The magnetic hook. Hooks onto a block, so that electromasters can move towards it.
 * @author WeathFolD
 *
 */
public class EntityMagHook extends EntityAdvanced
{

    private static final EntityDataAccessor<Boolean> IS_HIT = SynchedEntityData.defineId(EntityMagHook.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Direction> HIT_SIDE = SynchedEntityData.defineId(EntityMagHook.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<BlockPos> HOOK_POS = SynchedEntityData.defineId(EntityMagHook.class, EntityDataSerializers.BLOCK_POS);

    {
        Rigidbody rb = new Rigidbody();
        rb.gravity = 0.05;
        addMotionHandler(rb);
    }

    public boolean isHit;
    public Direction hitSide = Direction.DOWN;
    public int hookX, hookY, hookZ;

    boolean doesSetStill;

    public EntityMagHook(final Player player) {
        this(ACEntities.MAG_HOOK, player.level());
        setPosition(player.getX(), player.getEyeY(), player.getZ());
        Vec3 look = player.getLookAngle();
        setDeltaMovement(look.x * 2, look.y * 2, look.z * 2);

        setYRot(player.getYRot());
        setXRot(player.getXRot());

        Rigidbody rb = this.getMotionHandler(Rigidbody.class);
        rb.entitySel = EntitySelectors.exclude(player);

        this.regEventHandler(new CollideEvent.CollideHandler() {

            @Override
            public void onEvent(CollideEvent event) {
                RayTraceResult res = event.result;
                if(res.typeOfHit == RayTraceResult.Type.ENTITY) {
                    if(!(res.entityHit instanceof EntityMagHook) || ((EntityMagHook)res.entityHit).isHit) {
                        if(!(res.entityHit instanceof EntityMagHook) && world instanceof ServerLevel sl)
                            res.entityHit.hurtServer(sl, player.damageSources().playerAttack(player), 4);
                        dropAsItem();
                    }
                } else {
                    isHit = true;
                    hitSide = res.sideHit;
                    hookX = res.getBlockPos().getX();
                    hookY = res.getBlockPos().getY();
                    hookZ = res.getBlockPos().getZ();
                    setStill();
                }
            }

        });
    }

    public EntityMagHook(EntityType<? extends EntityMagHook> type, Level world) {
        super(type, world);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(HIT_SIDE, Direction.DOWN);
        builder.define(IS_HIT, false);
        builder.define(HOOK_POS, BlockPos.ZERO);
    }

    @Override
    public void onUpdate() {
        if(this.doesSetStill) {
            doesSetStill = false;
            realSetStill();
        }
        super.onUpdate();
        sync();
    }

    private void sync() {
        if(world.isClientSide()) {
            boolean lastHit = isHit;

            hitSide = entityData.get(HIT_SIDE);
            isHit = entityData.get(IS_HIT);

            BlockPos p = entityData.get(HOOK_POS);
            hookX = p.getX();
            hookY = p.getY();
            hookZ = p.getZ();
            if(!lastHit && isHit) {
                setStill();
            }
        } else {
            entityData.set(HIT_SIDE, hitSide);
            entityData.set(IS_HIT, isHit);
            entityData.set(HOOK_POS, new BlockPos(hookX, hookY, hookZ));
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource ds, float dmg) {
        if(isHit && ds.getEntity() instanceof Player) {
            dropAsItem();
        }
        return true;
    }

    @Override
    public boolean isPickable() {
        return this.isHit && !isRemoved();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return isHit ? EntityDimensions.scalable(1f, 1f) : super.getDimensions(pose);
    }

    private void dropAsItem() {
        if (!world.isClientSide()) {
            world.addFreshEntity(new ItemEntity(world, getX(), getY(), getZ(), new ItemStack(ACItems.mag_hook)));
        }
        setDead();
    }

    private void setStill() {
        this.doesSetStill = true;
    }

    private void realSetStill() {
        setDeltaMovement(0, 0, 0);
        this.playSound(Resources.sound("maghook_land"), .8f, 1.0f);
        this.refreshDimensions();
        this.removeMotionHandlers();
        this.addMotionHandler(new MotionHandler() {

            @Override
            public void onStart() {}

            @Override
            public void onUpdate() {
                preRender();
                if(!world.isClientSide()) {
                    //Check block consistency
                    if(world.getBlockState(new BlockPos(hookX, hookY, hookZ)).isAir()) {
                        dropAsItem();
                    }
                }
            }

            @Override
            public String getID() {
                return "huh";
            }

        });
    }

    @Override
    public void writeEntityToNBT(CompoundTag tag) {
        tag.putBoolean("isHit", isHit);
        tag.putInt("hitSide", hitSide.get3DDataValue());
        tag.putInt("hookX", hookX);
        tag.putInt("hookY", hookY);
        tag.putInt("hookZ", hookZ);
    }

    @Override
    public void readEntityFromNBT(CompoundTag tag) {
        isHit = tag.getBooleanOr("isHit", false);
        hitSide = Direction.from3DDataValue(tag.getIntOr("hitSide", 0));
        hookX = tag.getIntOr("hookX", 0);
        hookY = tag.getIntOr("hookY", 0);
        hookZ = tag.getIntOr("hookZ", 0);

        if(isHit) {
            setStill();
        }
    }

    public void preRender() {
        if(this.isHit) {
            switch(hitSide) {
            case DOWN -> setXRot(-90);
            case UP -> setXRot(90);
            case NORTH -> { setYRot(0); setXRot(0); }
            case SOUTH -> { setYRot(180); setXRot(0); }
            case WEST -> { setYRot(-90); setXRot(0); }
            case EAST -> { setYRot(90); setXRot(0); }
            }
            setPosition(hookX + 0.5 + hitSide.getStepX() * 0.51,
                    hookY + 0.5 + hitSide.getStepY() * 0.51,
                    hookZ + 0.5 + hitSide.getStepZ() * 0.51);
        }
    }

}
