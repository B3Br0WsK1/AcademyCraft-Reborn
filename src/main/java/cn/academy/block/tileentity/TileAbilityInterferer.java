package cn.academy.block.tileentity;

import cn.academy.ACTiles;
import cn.academy.block.block.BlockAbilityInterferer;
import cn.academy.datapart.CPData;
import cn.academy.energy.IFConstants;
import cn.academy.support.EnergyItemHelper;
import cn.lambdalib2.s11n.network.Future;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.TargetPoints;
import cn.lambdalib2.util.EntitySelectors;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.Side;
import cn.lambdalib2.util.TickScheduler;
import cn.lambdalib2.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Ability interferer. Players (that are not whitelisted) within its range can't use their abilities.
 *  Ported from the Scala version.
 * @author Paindar
 */
public class TileAbilityInterferer extends TileReceiverBase implements WorldlyContainer {

    public static final double minRange = 10.0;
    public static final double maxRange = 100.0;

    public static final String MSG_SYNC = "sync";
    public static final String MSG_UPDATE_RANGE = "set_range";
    public static final String MSG_UPDATE_WHITELIST = "set_whitelist";
    public static final String MSG_UPDATE_ENABLED = "set_enabled";
    public static final int SLOT_BATTERY = 0;

    private static final int[] SLOTS = { SLOT_BATTERY };

    private final TickScheduler scheduler = new TickScheduler();

    private boolean enabled_ = false;
    private @Nullable String placer_ = null;
    private SortedSet<String> whitelist_ = new TreeSet<>();
    private double range_ = minRange;

    public TileAbilityInterferer(BlockPos pos, BlockState state) {
        super(ACTiles.ABILITY_INTERFERER, pos, state, "ability_interferer", 1, 10000, IFConstants.LATENCY_MK1);

        // Check player in the area and interfere them
        scheduler.every(10).atOnly(Side.SERVER).condition(() -> enabled_).run(() -> {
            if (cost()) {
                AABB boundingBox = testBB();
                String sourceName = getSourceName();
                for (Entity e : WorldUtils.getEntities(getWorld(), boundingBox, EntitySelectors.survivalPlayer())) {
                    if (e instanceof Player player && !whitelist_.contains(player.getName().getString())) {
                        CPData.get(player).addInterf(sourceName, () ->
                            boundingBox.contains(player.position()) &&
                                !isRemoved() &&
                                !player.getAbilities().instabuild &&
                                enabled_);
                    }
                }
            } else {
                setEnabled(false);
            }
        });

        // Sync data to client
        scheduler.every(20).atOnly(Side.SERVER).run(this::sync);
    }

    private String getSourceName() {
        return "interferer@" + getWorld().dimension().identifier() + " " + getPos();
    }

    private AABB testBB() {
        BlockPos pos = getPos();
        return WorldUtils.minimumBounds(
            new Vec3(pos.getX() + 0.5 - range_, pos.getY() + 0.5 - range_, pos.getZ() + 0.5 - range_),
            new Vec3(pos.getX() + 0.5 + range_, pos.getY() + 0.5 + range_, pos.getZ() + 0.5 + range_));
    }

    public boolean enabled() {
        return enabled_;
    }

    public double range() {
        return range_;
    }

    public SortedSet<String> whitelist() {
        return whitelist_;
    }

    public @Nullable String placer() {
        return placer_;
    }

    public void setPlacer(Player p) {
        if (placer_ == null) {
            whitelist_.add(p.getName().getString());
            placer_ = p.getName().getString();
            setChanged();
        }
    }

    private void send(String channel, Object... args) {
        if (getWorld().isClientSide()) {
            NetworkMessage.sendToServer(this, channel, args);
        } else {
            NetworkMessage.sendToAllAround(TargetPoints.convert(this, 15), this, channel, args);
        }
    }

    private boolean cost() {
        energy -= range_ * range_;
        if (energy > 0) {
            return true;
        }
        energy = 0;
        return false;
    }

    private void sync() {
        send(MSG_SYNC, range_, enabled_, whitelist_.toArray(new String[0]));
    }

    private void setEnabled(boolean value) {
        enabled_ = value;
        setChanged();
        BlockState state = getBlockState();
        if (state.getBlock() instanceof BlockAbilityInterferer && state.getValue(BlockAbilityInterferer.PROP_ON) != value) {
            getWorld().setBlock(getPos(), state.setValue(BlockAbilityInterferer.PROP_ON, value), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void update() {
        super.update();
        scheduler.runTick();
        if (!getWorld().isClientSide()) {
            ItemStack stack = this.getStackInSlot(SLOT_BATTERY);
            if (!stack.isEmpty() && EnergyItemHelper.isSupported(stack)) {
                double gain = EnergyItemHelper.pull(stack, Math.min(getMaxEnergy() - getEnergy(), getBandwidth()), false);
                this.injectEnergy(gain);
            }
        }
    }

    @Listener(channel = MSG_SYNC, side = Side.CLIENT)
    private void hSync(double range2, boolean enabled2, String[] whitelist2) {
        range_ = range2;
        enabled_ = enabled2;
        whitelist_ = new TreeSet<>(Arrays.asList(whitelist2));
    }

    // Network-cross modifiers
    private static Future<Boolean> signalFuture(Runnable cb) {
        return Future.create(b -> cb.run());
    }

    public void setRangeClient(double value, Runnable callback) {
        send(MSG_UPDATE_RANGE, value, signalFuture(callback));
    }

    public void setEnabledClient(boolean value, Runnable callback) {
        send(MSG_UPDATE_ENABLED, value, signalFuture(callback));
    }

    public void setWhitelistClient(Collection<String> value, Runnable callback) {
        send(MSG_UPDATE_WHITELIST, value.toArray(new String[0]), signalFuture(callback));
    }

    @Listener(channel = MSG_UPDATE_RANGE, side = Side.SERVER)
    private void hSetRange(double value, Future<Boolean> fut) {
        range_ = MathUtils.clampd(minRange, maxRange, value);
        setChanged();
        fut.sendResult(true);
    }

    @Listener(channel = MSG_UPDATE_ENABLED, side = Side.SERVER)
    private void hSetEnabled(boolean value, Future<Boolean> fut) {
        setEnabled(value);
        fut.sendResult(true);
    }

    @Listener(channel = MSG_UPDATE_WHITELIST, side = Side.SERVER)
    private void hSetWhitelist(String[] value, Future<Boolean> fut) {
        whitelist_ = new TreeSet<>(Arrays.asList(value));
        setChanged();
        fut.sendResult(true);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == SLOT_BATTERY && EnergyItemHelper.isSupported(stack);
    }

    @Override
    public void readFromNBT(CompoundTag tag) {
        super.readFromNBT(tag);
        enabled_ = tag.getBooleanOr("enabled_", false);
        whitelist_ = new TreeSet<>();
        for (Tag t : tag.getListOrEmpty("whitelist_")) {
            t.asString().ifPresent(whitelist_::add);
        }
        placer_ = tag.getString("placer_").orElse(null);
        range_ = MathUtils.clampd(minRange, maxRange, tag.getFloatOr("range_", (float) minRange));
    }

    @Override
    public CompoundTag writeToNBT(CompoundTag tag) {
        super.writeToNBT(tag);
        tag.putBoolean("enabled_", enabled_);
        ListTag list = new ListTag();
        for (String s : whitelist_) {
            list.add(StringTag.valueOf(s));
        }
        tag.put("whitelist_", list);
        if (placer_ != null) {
            tag.putString("placer_", placer_);
        }
        tag.putFloat("range_", (float) range_);
        return tag;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack itemStackIn, @Nullable Direction direction) {
        return isItemValidForSlot(index, itemStackIn);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return false;
    }
}
