package cn.academy.energy.impl;
import cn.academy.AcademyCraft;
import cn.academy.energy.api.block.*;
import cn.academy.energy.impl.VBlocks.*;
import cn.lambdalib2.util.IBlockSelector;
import cn.lambdalib2.util.WorldUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import java.util.*;

//import net.minecraft.world.WorldSavedData;


/**
 * @author WeAthFolD
 *
 */
public class WiWorldData extends SavedData {

    public static final SavedDataType<WiWorldData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath("academy", "wireless_energy_network"),
        WiWorldData::new,
        CompoundTag.CODEC.xmap(tag -> {
            WiWorldData data = new WiWorldData();
            data.readFromNBT(tag);
            return data;
        }, data -> data.writeToNBT(new CompoundTag())),
        null);

    //Set by get method, which should be the ONLY way to access WiWorldData
    Level world;

    public WiWorldData() {
    }

    //-----WEN-----
    
    private IBlockSelector filterWirelessBlocks = new IBlockSelector() {

        @Override
        public boolean accepts(Level world, int x, int y, int z, Block block) {
            BlockEntity te = world.getBlockEntity(new BlockPos(x, y, z));
            return te instanceof IWirelessMatrix || te instanceof IWirelessNode;
        }
        
    };
    
    /***
     * Object valid for lookup: VWMatrix, VWNode
     */
    Map<Object, WirelessNet> netLookup = new HashMap<>();
    Set<WirelessNet> netList = new HashSet<>();
    
    /**
     * Internal, used to prevent concurrent modification.
     */
    private List<WirelessNet> toRemove = new ArrayList<>();
    
    private void tickNetwork() {
        for(WirelessNet net : toRemove) {
            this.doRemoveNetwork(net);
        }
        toRemove.clear();
        
        Iterator<WirelessNet> iter = netList.iterator();
        while (iter.hasNext()) {
            WirelessNet net = iter.next();
            if(net.isDisposed()) {
                toRemove.add(net);
            } else {
                net.world = world;
                net.tick();
            }
        }
    }
    
    boolean createNetwork(IWirelessMatrix matrix, String ssid, String password) {
        // Kill old net of the same matrix, if any
        VWMatrix vm = new VWMatrix(matrix);
        if(netLookup.containsKey(vm)) {
            WirelessNet old = netLookup.get(vm);
            doRemoveNetwork(old);
        }
        
        //Add new
        WirelessNet net = new WirelessNet(this, vm, ssid, password);
        doAddNetwork(net);
        
        return true;
    }

    private final List<BlockPos> _bufferBlockPos = new ArrayList<>();
    
    public Collection<WirelessNet> rangeSearch(int x, int y, int z, double range, int max) {
        WorldUtils.getBlocksWithin(_bufferBlockPos, world, x, y, z, range, max, filterWirelessBlocks);
        
        Set<WirelessNet> set = new HashSet<>();
        for(BlockPos bp : _bufferBlockPos) {
            BlockEntity te = world.getBlockEntity(bp);
            WirelessNet net;
            if(te instanceof IWirelessMatrix) {
                net = getNetwork((IWirelessMatrix) te);
            } else if(te instanceof IWirelessNode) {
                net = getNetwork((IWirelessNode) te);
            } else {
                throw new RuntimeException("Invalid BlockEntity");
            }
            if(net != null && net.isInRange(x, y, z) && net.getLoad() < net.getCapacity()) {
                set.add(net);
                if(set.size() >= max)
                    return set;
            }
        }

        return set;
    }
    
    public WirelessNet getNetwork(IWirelessMatrix matrix) {
        return privateGetNetwork(new VWMatrix(matrix));
    }
    
    public WirelessNet getNetwork(IWirelessNode node) {
        return privateGetNetwork(new VWNode(node));
    }

    private WirelessNet privateGetNetwork(Object key) {
        WirelessNet ret = netLookup.get(key);
        if (ret != null && ret.validate()) {
            return ret;
        } else {
            return null;
        }
    }
    
    private void doRemoveNetwork(WirelessNet net) {
        netList.remove(net);
        net.onCleanup(this);
    }
    
    private void doAddNetwork(WirelessNet net) {
        netList.add(net);
        net.onCreate(this);
    }
    
    private void loadNetwork(CompoundTag tag) {
        ListTag list = tag.getListOrEmpty("networks");
        for(int i = 0; i < list.size(); ++i) {
            CompoundTag tag2 = list.getCompoundOrEmpty(i);
            WirelessNet net = new WirelessNet(this, tag2);
            doAddNetwork(net);
        }
    }
    
    private void saveNetwork(CompoundTag tag) {
        ListTag list = new ListTag();
        for(WirelessNet net : netList) {
            if(!net.isDisposed()) {
                list.add(net.toNBT());
            }
        }
        tag.put("networks", list);
    }
    
    //-----NodeConn----
    Map<Object, NodeConn> nodeLookup = new HashMap<>();
    Set<NodeConn> nodeList = new HashSet<>();
    List<NodeConn> nToRemove = new ArrayList<>();
    
    /**
     * Get the node connection of a node. If not found will create a new one. Never returns null.
     */
    public NodeConn getNodeConnection(IWirelessNode node) {
        VNNode vnn = new VNNode(node);
        NodeConn ret = privateGetNodeConn(vnn);
        if(ret == null) {
            doAddNode(ret = new NodeConn(this, vnn));
        }
        return ret;
    }
    
    public NodeConn getNodeConnection(IWirelessUser user) {
        if(user instanceof IWirelessGenerator) {
            return privateGetNodeConn(new VNGenerator((IWirelessGenerator) user));
        } else if(user instanceof IWirelessReceiver) {
            return privateGetNodeConn(new VNReceiver((IWirelessReceiver) user));
        } else if (user == null) {
            return null;
        } else {
            throw new IllegalArgumentException("Invalid user type");
        }
    }

    private NodeConn privateGetNodeConn(Object key) {
        NodeConn ret = nodeLookup.get(key);
        if (ret != null && ret.validate()) {
            return ret;
        } else {
            return null;
        }
    }
    
    private void tickNode() {
        //if(true) return;
        
        for(NodeConn nc : nToRemove) {
            doRemoveNode(nc);
        }
        nToRemove.clear();
        
        Iterator<NodeConn> iter = nodeList.iterator();
        while(iter.hasNext()) {
            NodeConn conn = iter.next();
            if(conn.isDisposed()) {
                nToRemove.add(conn);
            } else {
                conn.tick();
            }
        }
    }
    
    private void doAddNode(NodeConn conn) {
        nodeList.add(conn);
        conn.onAdded(this);
    }
    
    private void doRemoveNode(NodeConn conn) {
        nodeList.remove(conn);
        conn.onCleanup(this);
    }
    
    private void loadNode(CompoundTag tag) {
        ListTag list = tag.getListOrEmpty("list");
        debug("LoadNode " + list + (list.size()));
        for(int i = 0; i < list.size(); ++i) {
            doAddNode(new NodeConn(this, list.getCompoundOrEmpty(i)));
        }
    }
    
    private void saveNode(CompoundTag tag) {
        ListTag list = new ListTag();
        for(NodeConn c : nodeList) {
            if(!c.isDisposed()) {
                list.add(c.toNBT());
            }
        }
        tag.put("list", list);
    }
    
    //-----Generic-----
    public void tick() {
        tickNetwork();
        tickNode();
    }
    
    public void readFromNBT(CompoundTag tag) {
        tag.getCompound("net").ifPresent(this::loadNetwork);
        tag.getCompound("node").ifPresent(this::loadNode);
    }

    public CompoundTag writeToNBT(CompoundTag tag) {
        CompoundTag tag1 = new CompoundTag();
        saveNetwork(tag1);
        tag.put("net", tag1);

        tag1 = new CompoundTag();
        saveNode(tag1);
        tag.put("node", tag1);
        return tag;
    }

    public static WiWorldData get(Level world) {
        if(world.isClientSide() || !(world instanceof ServerLevel sl)) {
            throw new RuntimeException("Not allowed to create WiWorldData in client");
        }
        WiWorldData ret = sl.getDataStorage().computeIfAbsent(TYPE);
        ret.world = world;
        return ret;
    }

    public static WiWorldData getNonCreate(Level world) {
        if (!(world instanceof ServerLevel sl)) return null;
        WiWorldData data = sl.getDataStorage().get(TYPE);
        if(data != null) data.world = world;
        return data;
    }

    @Override
    public boolean isDirty() {
        return true;
    }

    private void debug(Object msg) {
        AcademyCraft.log.info("WiWorldData: " + msg);
    }

}