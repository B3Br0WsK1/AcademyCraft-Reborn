package cn.academy.entity;
import cn.academy.ACItems;
import cn.academy.AcademyCraft;
import cn.academy.event.ConfigModifyEvent;
import cn.academy.ability.vanilla.VanillaCategories;
import cn.academy.client.render.entity.RendererCoinThrowing;
import cn.academy.item.ItemCoin;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.s11n.network.NetworkS11n;
import cn.lambdalib2.util.PlayerUtils;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.entityx.EntityAdvanced;
import cn.lambdalib2.util.entityx.MotionHandler;
import cn.lambdalib2.util.entityx.handlers.Rigidbody;
import io.netty.buffer.ByteBuf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import cn.academy.ACEntities;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import cn.lambdalib2.event.EventHub;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.event.SubscribeEvent;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;


/**
 * 
 * @author KSkun
 */
public class EntityCoinThrowing extends EntityAdvanced
{

    public static boolean PLAY_HEADS_OR_TAILS;

    static {
        NetworkS11n.addDirect(EntityCoinThrowing.class, new NetworkS11n.NetS11nAdaptor<EntityCoinThrowing>() {
            @Override
            public void write(ByteBuf buf, EntityCoinThrowing obj) {
                NetworkS11n.serializeWithHint(buf, obj.player, Player.class);
            }

            @Override
            public EntityCoinThrowing read(ByteBuf buf){
                return ItemCoin.getPlayerCoin(NetworkS11n.deserializeWithHint(buf, Player.class));
            }
        });
    }

    double yOffset = 0.6;
    
    private class KeepPosition extends MotionHandler<EntityCoinThrowing>
    {

        public KeepPosition() {}

        @Override
        public void onUpdate() {
            if(EntityCoinThrowing.this.player != null) {
                setPos(player.getX(), getY(), getZ());
                setPos(getX(), getY(), player.getZ());
                if((getY() < player.getY() && getDeltaMovement().y < 0) || tickCount > MAXLIFE) {
                    finishThrowing();
                }
            }
            
            maxHt = Math.max(maxHt, getY());
        }

        @Override
        public String getID() {
            return "kip";
        }

        @Override
        public void onStart() {}
        
    }

    private static final int MAXLIFE = 120;
    private static final double INITVEL = 0.92;
    
    //private EntitySyncer syncer;
    
    //@Synchronized(SyncType.ONCE)
    private float initHt;
    private double maxHt;
    
    //@Synchronized(SyncType.ONCE)
    public Player player;
    
    public ItemStack stack;
    public Vec3 axis;
    public boolean isSync = false;
    
    public EntityCoinThrowing(EntityType<? extends EntityCoinThrowing> type, Level world) {
        super(type, world);
        isSync = true;
        setup();
    }
    
    public EntityCoinThrowing(Player player, ItemStack is) {
        super(ACEntities.COIN_THROWING, player.level());
        this.stack = is;
        this.player = player;
        this.initHt = (float) player.getY();
        this.maxHt = initHt;
        setPosition(player.getX(), player.getY(), player.getZ());
        this.setDeltaMovement(this.getDeltaMovement().x, player.getDeltaMovement().y, this.getDeltaMovement().z);
        setup();
    }
    
    @Override
    public void onUpdate() {
        if (world.isClientSide() && isSync)
            setDead();
        //if(!world.isClientSide() || isSync)
            //syncer.update();
        //System.out.println(initHt + " " + player + " " + world.isClientSide());
        super.onUpdate();
    }
    
    private void setup() {
        Rigidbody rb = new Rigidbody();
        rb.gravity = 0.06;
        this.addMotionHandler(rb);
        this.addMotionHandler(new KeepPosition());
        this.setDeltaMovement(this.getDeltaMovement().x, this.getDeltaMovement().y + (INITVEL), this.getDeltaMovement().z);
        axis = new Vec3(.1 + rand.nextDouble(), rand.nextDouble(), rand.nextDouble());
    }
    
    void finishThrowing() {
        //try merge
        if(!world.isClientSide() && player != null && !player.getAbilities().instabuild) {
            ItemStack equipped = player.getMainHandItem();
            if (equipped.isEmpty()) {
                player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ACItems.coin));
            } else if (equipped.getItem() == ACItems.coin && equipped.getCount() < equipped.getMaxStackSize()) {
                equipped.setCount(equipped.getCount()+1);
//                player.getInventory().inventoryChanged = true;
                player.getInventory().setChanged();
            } else if (PlayerUtils.mergeStackable(player.getInventory(), new ItemStack(
                    ACItems.coin)) == 0) {
                ;
            } else {
                //if fail...
                world.addFreshEntity(new ItemEntity(world, player.getX(), player.getY()
                    + yOffset, player.getZ(), new ItemStack(ACItems.coin)));
            }
        }
        if (world.isClientSide() && PLAY_HEADS_OR_TAILS && player != null) {
            player.sendOverlayMessage(Component.translatable(
                "ac.headsOrTails." + RandUtils.nextInt(2)));
        }
        setDead();
    }
    
    public double getProgress() {
        if(getDeltaMovement().y > 0) { //Throwing up
            return (INITVEL - getDeltaMovement().y) / INITVEL * 0.5;
        } else {
            return Math.min(1.0, 0.5 + ((maxHt - getY()) / (maxHt - initHt)) * 0.5);
        }
    }
    
    @Override
    protected void readEntityFromNBT(CompoundTag tag) {
        // A saved coin is turned back into an item when loaded
        setDead();
        if (!world.isClientSide()) {
            world.addFreshEntity(new ItemEntity(world, getX(), getY(), getZ(), new ItemStack(ACItems.coin)));
        }
    }

    @Override
    protected void writeEntityToNBT(CompoundTag p_70014_1_) {
        
    }

    public enum EventListener {
        instance;

        @StateEventCallback
        private static void init(StateEvents.Init event) {
            PLAY_HEADS_OR_TAILS = AcademyCraft.config.getBoolean("headsOrTails",
                "generic", false, "Show heads or tails after throwing a coin.");
            EventHub.BUS.register(instance);
        }

        @SubscribeEvent
        public void onConfigModified(ConfigModifyEvent e) {
            PLAY_HEADS_OR_TAILS = AcademyCraft.config.getBoolean("headsOrTails",
                "generic", false, "Show heads or tails after throwing a coin.");
        }
    }

}