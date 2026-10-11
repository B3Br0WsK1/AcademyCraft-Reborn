package cn.lambdalib2.template.tile;

import cn.lambdalib2.util.SideUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A BlockEntity base using the 1.12 style NBT callbacks ({@link #readFromNBT} / {@link #writeToNBT}), which are used
 *  for saving, for the initial client sync and can be used for custom sync messages.
 *  Tickable tile entities implement {@link ITickableTile}.
 */
public abstract class TileEntityBase extends BlockEntity {

    private static final String TAG_KEY = "lambdalib2_data";

    public TileEntityBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * Reads the tile's state from NBT.
     */
    public void readFromNBT(CompoundTag tag) {}

    /**
     * Writes the tile's state into NBT.
     * @return The tag given
     */
    public CompoundTag writeToNBT(CompoundTag tag) {
        return tag;
    }

    @Override
    protected final void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store(TAG_KEY, CompoundTag.CODEC, writeToNBT(new CompoundTag()));
    }

    @Override
    protected final void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        readFromNBT(input.read(TAG_KEY, CompoundTag.CODEC).orElseGet(CompoundTag::new));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ---- 1.12 style helpers

    public Level getWorld() {
        return level;
    }

    public BlockPos getPos() {
        return worldPosition;
    }

    public Block getBlockType() {
        return getBlockState().getBlock();
    }

    public void markDirty() {
        setChanged();
    }

    /**
     * Sends the block entity data to the clients tracking it.
     */
    public void syncToClients() {
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    public HolderLookup.Provider registries() {
        if (level != null) {
            return level.registryAccess();
        }
        if (SideUtils.getServer() != null) {
            return SideUtils.getServer().registryAccess();
        }
        throw new IllegalStateException("No registry access available for " + this);
    }

    /**
     * Serializes an item stack (may be empty) into an NBT tag.
     */
    public Tag writeStack(ItemStack stack) {
        RegistryOps<Tag> ops = registries().createSerializationContext(NbtOps.INSTANCE);
        return ItemStack.OPTIONAL_CODEC.encodeStart(ops, stack).getOrThrow();
    }

    public ItemStack readStack(Tag tag) {
        if (tag == null) return ItemStack.EMPTY;
        RegistryOps<Tag> ops = registries().createSerializationContext(NbtOps.INSTANCE);
        return ItemStack.OPTIONAL_CODEC.parse(ops, tag).result().orElse(ItemStack.EMPTY);
    }

}
