package cn.academy.entity;

import cn.academy.ACEntities;
import cn.academy.AcademyCraft;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.TargetPoints;
import cn.lambdalib2.util.RayTraceResult;
import cn.lambdalib2.util.Side;
import cn.lambdalib2.util.entityx.EntityAdvanced;
import cn.lambdalib2.util.entityx.event.CollideEvent;
import cn.lambdalib2.util.entityx.handlers.Rigidbody;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;

/**
 * An entity that renders block.
 * @author WeAthFolD
 */
public class EntityBlock extends EntityAdvanced {

    /**
     * Create an EntityBlock from the block given in the coordinate.
     * DOESN't set the coordinate, do it yourself!
     * @return An setup entity block, or null if convert failed.
     */
    public static EntityBlock convert(Level world, BlockPos pos) {
        EntityBlock ret = new EntityBlock(world);
        if(!ret.setBlock(world.getBlockState(pos).getBlock(), world.getBlockState(pos)))
            return null;
        ret.setTileEntity(world.getBlockEntity(pos));
        return ret;
    }

    private static final EntityDataAccessor<BlockState> BLOCK_STATE =
        SynchedEntityData.defineId(EntityBlock.class, EntityDataSerializers.BLOCK_STATE);

    public Block block;
    public BlockState _blockState;
    /**
     * The block entity of the block. In server, only its saved data ({@link #tileTag}) is used.
     *  In client it is a detached copy used for rendering.
     */
    public BlockEntity tileEntity;
    public CompoundTag tileTag;

    // other
    public boolean placeWhenCollide = true;

    Player player;

    public float yaw, lastYaw, pitch, lastPitch;

    /**
     * Server ctor. We use a player parameter as the placer of the block.
     */
    public EntityBlock(Player _player) {
        this(ACEntities.ENTITY_BLOCK, _player.level());
        player = _player;
    }

    public EntityBlock(Level world) {
        this(ACEntities.ENTITY_BLOCK, world);
    }

    public EntityBlock(EntityType<? extends EntityBlock> type, Level world) {
        super(type, world);
        Rigidbody rb = new Rigidbody();
        rb.accurateCollision = true;
        this.addMotionHandler(rb);
    }

    // Additional hack for scala...
    public void constructServer(Player _player) {
        player = _player;
    }

    // Hack...
    public void setPlaceFromServer(boolean value) {
        placeWhenCollide = value;

        NetworkMessage.sendToAllAround(
                TargetPoints.convert(this, 20),
                this, "spfs", value
        );
    }

    @NetworkMessage.Listener(channel="spfs", side={Side.CLIENT})
    private void hSpfs(boolean value) {
        placeWhenCollide = value;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(BLOCK_STATE, Blocks.AIR.defaultBlockState());
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        lastYaw = yaw;
        lastPitch = pitch;

        if(world.isClientSide()) {
            BlockState state = entityData.get(BLOCK_STATE);
            if (!state.isAir()) {
                _blockState = state;
                block = state.getBlock();
            }
        } else {
            if(block != null && entityData.get(BLOCK_STATE) != _blockState) {
                entityData.set(BLOCK_STATE, _blockState);
            }
        }
    }

    private boolean canReplace(BlockPos pos) {
        return world.getBlockState(pos).canBeReplaced();
    }

    private void placeAt(BlockPos pos) {
        world.setBlock(pos, _blockState, Block.UPDATE_ALL);
        if (tileTag != null) {
            BlockEntity te = world.getBlockEntity(pos);
            if (te != null) {
                try {
                    te.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, world.registryAccess(), tileTag));
                    te.setChanged();
                } catch (Exception e) {
                    AcademyCraft.log.error("Failed to restore block entity data", e);
                }
            }
        }
    }

    @Override
    public void onFirstUpdate() {
        this.regEventHandler(new CollideEvent.CollideHandler() {
            @Override
            public void onEvent(CollideEvent event) {
                if(placeWhenCollide && !world.isClientSide() && event.result.typeOfHit == RayTraceResult.Type.BLOCK
                        && _blockState != null) {
                    BlockPos originPos = event.result.getBlockPos();
                    Direction side = event.result.sideHit;
                    int tx = originPos.getX(), ty = originPos.getY(), tz = originPos.getZ();

                    boolean isPlace = false;
                    BlockPos placePos = originPos;
                    if(canReplace(originPos)) {
                        isPlace = true;
                    } else if(canReplace(originPos.relative(side))) {
                        isPlace = true;
                        placePos = originPos.relative(side);
                    } else {
                        for(int x = -1; x <= 1 && !isPlace; x++) {
                            for(int y = -1; y <= 1 && !isPlace; y++) {
                                for(int z = -1; z <= 1 && !isPlace; z++) {
                                    if (x != 0 && y != 0 && z != 0 && canReplace(originPos.offset(x, y, z))) {
                                        isPlace = true;
                                        placePos = originPos.offset(x, y, z);
                                    }
                                }
                            }
                        }
                    }

                    if (isPlace) {
                        placeAt(placePos);
                    } else {
                        int iter = 10;
                        while (iter --> 0) {
                            BlockPos p = new BlockPos(tx, ty, tz);
                            if(!canReplace(p)) {
                                tx += side.getStepX();
                                ty += side.getStepY();
                                tz += side.getStepZ();
                            } else {
                                placeAt(p);
                                isPlace = true;
                                break;
                            }
                        }
                    }
                    if (!isPlace) {
                        AcademyCraft.log.error("EntityBlock Lost: " + event.result);
                    }
                    setDead();
                }
            }

        });

        if(!world.isClientSide() && tileTag != null) {
            NetworkMessage.sendToAllAround(TargetPoints.convert(this, 64), this, "sync_te", tileTag);
        }
    }

    public boolean isAvailable() {
        return block != null;
    }

    public void setBlock(Block _block) {
        block = _block;
        _blockState = block.defaultBlockState();
    }

    public boolean setBlock(Block _block, BlockState blockState) {
        if(_block.asItem() == net.minecraft.world.item.Items.AIR)
            return false;
        block = _block;
        _blockState = blockState;
        return true;
    }

    public void setTileEntity(BlockEntity _te) {
        tileEntity = _te;
        tileTag = _te == null ? null : _te.saveWithFullMetadata(world.registryAccess());
    }

    public boolean shouldRender() {
        return block != null;
    }

    @Override
    protected void readEntityFromNBT(CompoundTag tag) {
        setDead();
    }

    @NetworkMessage.Listener(channel="sync_te", side=Side.CLIENT)
    private void hSyncTE(CompoundTag initTag) {
        try {
            BlockState state = _blockState != null ? _blockState : entityData.get(BLOCK_STATE);
            BlockEntity te = BlockEntity.loadStatic(blockPosition(), state, initTag, world.registryAccess());
            if (te != null) {
                te.setLevel(world);
            }
            tileEntity = te;
        } catch(Exception e) {
            AcademyCraft.log.error("Unable to sync tileEntity", e);
        }
    }

}
