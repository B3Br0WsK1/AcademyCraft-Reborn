package cn.academy.block.tileentity;

import cn.lambdalib2.template.tile.TileEntityBase;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * You better directly extend this class rather than delegating it. Too much interfaces could be a burden.
 * Implements vanilla {@link Container}, and keeps the 1.12 IInventory method names as aliases.
 * @author WeAthFolD
 */
public class TileInventory extends TileEntityBase implements Container {

    protected final ItemStack[] inventory;

    private final String invName;

    public TileInventory(BlockEntityType<?> type, BlockPos pos, BlockState state, String _invName, int size) {
        super(type, pos, state);
        inventory = new ItemStack[size];
        for(int i=0;i<size;i++)
        {
            inventory[i]=ItemStack.EMPTY;
        }
        invName = _invName;
    }

    public ItemStack[] getInventory() {
        return inventory;
    }

    // ---- Container

    @Override
    public int getContainerSize() {
        return inventory.length;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : inventory) {
            if (!s.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return inventory[slot];
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        if (!inventory[slot].isEmpty()) {
            ItemStack itemstack;

            if (inventory[slot].getCount() <= count) {
                itemstack = inventory[slot];
                inventory[slot] = ItemStack.EMPTY;
            } else {
                itemstack = inventory[slot].split(count);

                if (inventory[slot].getCount() == 0) {
                    inventory[slot] = ItemStack.EMPTY;
                }
            }
            this.setChanged();
            return itemstack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack itemstack = inventory[slot];
        inventory[slot] = ItemStack.EMPTY;
        return itemstack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inventory[slot] = stack;

        if (!stack.isEmpty() && stack.getCount() > this.getInventoryStackLimit()) {
            stack.setCount(this.getInventoryStackLimit());
        }

        this.setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return getInventoryStackLimit();
    }

    @Override
    public boolean stillValid(Player player) {
        return isUsableByPlayer(player);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isItemValidForSlot(slot, stack);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < inventory.length; ++i) {
            inventory[i] = ItemStack.EMPTY;
        }
    }

    // ---- 1.12 style aliases / overridables

    public int getSizeInventory() {
        return getContainerSize();
    }

    public ItemStack getStackInSlot(int slot) {
        return getItem(slot);
    }

    public ItemStack decrStackSize(int slot, int count) {
        return removeItem(slot, count);
    }

    public ItemStack removeStackFromSlot(int slot) {
        return removeItemNoUpdate(slot);
    }

    public void setInventorySlotContents(int slot, ItemStack stack) {
        setItem(slot, stack);
    }

    public int getInventoryStackLimit() {
        return 64;
    }

    public boolean isUsableByPlayer(Player player) {
        return true;
    }

    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    public String getName() {
        return invName;
    }

    // ---- NBT

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        CompoundTag tag = nbt.getCompoundOrEmpty("inventory");
        for(int i = 0; i < inventory.length; ++i) {
            String name = "" + i;
            inventory[i] = tag.contains(name) ? readStack(tag.get(name)) : ItemStack.EMPTY;
        }
    }

    @Override
    public CompoundTag writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        CompoundTag tag = new CompoundTag();
        for(int i = 0; i < inventory.length; ++i) {
            if(!inventory[i].isEmpty()) {
                tag.put("" + i, writeStack(inventory[i]));
            }
        }

        nbt.put("inventory", tag);
        return nbt;
    }
}
