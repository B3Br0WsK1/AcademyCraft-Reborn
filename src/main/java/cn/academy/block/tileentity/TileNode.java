package cn.academy.block.tileentity;

import cn.academy.ACTiles;
import cn.academy.block.block.BlockNode;
import cn.academy.block.block.BlockNode.NodeType;
import cn.academy.energy.api.IFItemManager;
import cn.academy.energy.api.WirelessHelper;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.impl.WirelessNet;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.TargetPoints;
import cn.lambdalib2.template.tile.ITickableTile;
import cn.lambdalib2.util.Side;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * @author WeathFolD
 *
 */
public class TileNode extends TileInventory implements IWirelessNode, ITickableTile {

    static IFItemManager itemManager = IFItemManager.instance;

    static final String MSG_SYNC = "sync";

    protected double energy;

    private int updateTicker;

    private String password = "";

    /**
     * Client-only flag. Only *roughly* indicates whether the block is linked.
     * Used for just rendering.
     */
    public boolean enabled = false;

    public boolean chargingIn = false;

    public boolean chargingOut = false;

    private String placerName = "";

    public TileNode(BlockPos pos, BlockState state) {
        super(ACTiles.NODE, pos, state, "wireless_node", 2);
    }

    public void setPlacer(Player player) {
        placerName = player.getName().getString();
    }

    public String getPlacerName() {
        return placerName;
    }

    @Override
    public void update() {
        if(!getWorld().isClientSide()) {
            ++updateTicker;
            if(updateTicker == 10) {
                updateTicker = 0;

                WirelessNet net = WirelessHelper.getWirelessNet(this);
                enabled = net != null;

                NetworkMessage.sendToAllAround(TargetPoints.convert(this, 20),
                        this, MSG_SYNC,
                        enabled, chargingIn, chargingOut, energy, name, password, placerName);
                rebuildBlockState();
            }

            updateChargeIn();
            updateChargeOut();
        }
    }

    public void setPassword(String _pass) {
        password = _pass;
        setChanged();
    }

    private void updateChargeIn() {
        ItemStack stack = this.getStackInSlot(0);
        if(!stack.isEmpty() && itemManager.isSupported(stack)) {
            //Charge into the node.
            double req = Math.min(getBandwidth(), getMaxEnergy() - energy);
            double pull = itemManager.pull(stack, req, false);

            chargingIn = pull != 0;
            this.setEnergy(energy + pull);
        } else {
            chargingIn = false;
        }
    }

    private void updateChargeOut() {
        ItemStack stack = this.getStackInSlot(1);
        if(!stack.isEmpty() && itemManager.isSupported(stack)) {
            double cur = getEnergy();
            if(cur > 0) {
                cur = Math.min(getBandwidth(), cur);
                double left = itemManager.charge(stack, cur);

                chargingOut = left != cur;
                this.setEnergy(getEnergy() - (cur - left));
            }
        } else {
            chargingOut = false;
        }
    }

    private void rebuildBlockState() {
        if (level == null || level.isClientSide())
            return;
        BlockState curState = level.getBlockState(worldPosition);
        Block block = curState.getBlock();
        // sometime block and tileentity are mismatch
        if (block instanceof BlockNode) {
            boolean lastConnected = curState.getValue(BlockNode.CONNECTED);
            int lastPct = curState.getValue(BlockNode.ENERGY);

            int pct = (int) Math.min(4, Math.round((4 * getEnergy() / getMaxEnergy())));
            if (pct != lastPct || lastConnected != enabled) {
                level.setBlock(worldPosition,
                        curState
                                .setValue(BlockNode.CONNECTED, enabled)
                                .setValue(BlockNode.ENERGY, pct),
                        Block.UPDATE_CLIENTS);
            }
        }

    }

    @Override
    public double getMaxEnergy() {
        return getNodeType().maxEnergy;
    }

    @Override
    public double getEnergy() {
        return energy;
    }

    @Override
    public void setEnergy(double value) {
        energy = value;
        setChanged();
        rebuildBlockState();
    }

    @Override
    public double getBandwidth() {
        return getNodeType().bandwidth;
    }

    @Override
    public double getRange() {
        return getNodeType().range;
    }

    public NodeType getNodeType() {
        Block block = getBlockState().getBlock();
        return block instanceof BlockNode node ? node.type : NodeType.BASIC;
    }

    @Override
    public void readFromNBT(CompoundTag tag) {
        super.readFromNBT(tag);
        energy = tag.getDoubleOr("energy", 0);
        name = tag.getStringOr("nodeName", "Unnamed");
        password = tag.getStringOr("password", "");
        placerName = tag.getStringOr("placer", "");
    }

    @Override
    public CompoundTag writeToNBT(CompoundTag tag) {
        super.writeToNBT(tag);
        tag.putDouble("energy", energy);
        tag.putString("nodeName", name);
        tag.putString("password", password);
        tag.putString("placer", placerName);
        return tag;
    }

    String name = "Unnamed";

    @Override
    public String getNodeName() {
        return name;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setNodeName(String name) {
        this.name = name;
        setChanged();
    }

    @Listener(channel=MSG_SYNC, side=Side.CLIENT)
    void hSync(boolean enabled, boolean chargingIn, boolean chargingOut,
               double energy, String name, String pass, String placerName) {
        this.enabled = enabled;
        this.chargingIn = chargingIn;
        this.chargingOut = chargingOut;
        this.energy = energy;
        this.name = name;
        this.password = pass;
        this.placerName = placerName;
    }

    @Override
    public int getCapacity() {
        return getNodeType().capacity;
    }

}
