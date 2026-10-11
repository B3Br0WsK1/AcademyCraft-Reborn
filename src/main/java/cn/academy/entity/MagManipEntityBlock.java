package cn.academy.entity;
import cn.academy.ability.AbilityContext;
import cn.academy.ability.vanilla.electromaster.skill.MagManip;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.VecUtils;
import cn.lambdalib2.util.entityx.MotionHandler;
import cn.lambdalib2.util.entityx.event.CollideEvent;
import cn.lambdalib2.util.entityx.handlers.Rigidbody;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import cn.academy.ACEntities;
import cn.lambdalib2.s11n.network.TargetPoints;
import net.minecraft.world.entity.EntityType;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
public class MagManipEntityBlock extends EntityBlock
{
    public static final int ActNothing=0, ActMoveTo=1;
    public float damage;
    public boolean placeWhenCollide = false;
    Player player2 = null;
    float yawSpeed = RandUtils.rangef(1, 3);
    float pitchSpeed = RandUtils.rangef(1, 3);
    private int tick =0;
    private final int MAXTICK = 5;

    public int actionType;
    float tx, ty, tz;

    public MagManipEntityBlock(Player _player, float damage)
    {
        super(ACEntities.MAG_MANIP_BLOCK, _player.level());
        constructServer(_player);
        this.damage = damage;
        this.player2 = _player;
    }

    public MagManipEntityBlock(Level world)
    {
        super(ACEntities.MAG_MANIP_BLOCK, world);
    }

    public MagManipEntityBlock(EntityType<? extends MagManipEntityBlock> type, Level world)
    {
        super(type, world);
    }

    public void setMoveTo(double x, double y, double z)
    {
        actionType = ActMoveTo;
        tx = (float) x;
        ty = (float) y;
        tz = (float) z;
    }

    public void stopMoveTo()
    {
        actionType = ActNothing;
    }

    @Override
    public void onFirstUpdate()
    {
        super.onFirstUpdate();
        Rigidbody rb = getMotionHandler(Rigidbody.class);
        rb.entitySel = entity -> entity != player2;

        regEventHandler(new CollideEvent.CollideHandler()
        {
            @Override
            public void onEvent(CollideEvent event)
            {
                if(!world.isClientSide() && event.result!=null && event.result.entityHit!=null)
                {
                    AbilityContext.of(player2, MagManip.INSTANCE).attack(event.result.entityHit, damage);
                }
            }
        });

        if( world.isClientSide())
        {
            startClient();
        }
        else
        {
            NetworkMessage.sendToAllAround(TargetPoints.convert(this, 40),
                    this, "MSG_ENT_SYNC", player2 == null ? -1 : player2.getId(), actionType, tx, ty, tz);
        }
    }

    @Listener(channel = "MSG_ENT_SYNC", side= Side.CLIENT)
    public void onClientSyncEntity(int id, int actType, float _tx, float _ty, float _tz)
    {
        player2 = world.getEntity(id) instanceof Player p ? p : null;
        actionType = actType;
        tx = _tx;
        ty = _ty;
        tz = _tz;
    }

    @Override
    public void onUpdate()
    {
        if(!world.isClientSide())
        {
            tick++;
            if(tick>MAXTICK)
            {
                tick=0;
                NetworkMessage.sendToAllAround(TargetPoints.convert(this, 40),
                        this, "MSG_ENT_SYNC", player2 == null ? -1 : player2.getId(), actionType, tx, ty, tz);
            }
        }
        super.onUpdate();

        yaw+=yawSpeed;
        pitch += pitchSpeed;
        switch(actionType)
        {
            case ActMoveTo:
                double dist = this.distanceToSqr(tx, ty, tz);
                Vec3 delta = new Vec3(tx - getX(), ty - getY(), tz - getZ()).normalize();
                Vec3 mo = delta.scale(0.2).scale(dist<4?dist/4:1.0);
                VecUtils.setMotion(this, mo);
                break;
            case ActNothing:
                setDeltaMovement(getDeltaMovement().x, getDeltaMovement().y - (0.04), getDeltaMovement().z);
        }
        setPos(getX() + (getDeltaMovement().x), getY(), getZ());
        setPos(getX(), getY() + (getDeltaMovement().y), getZ());
        setPos(getX(), getY(), getZ() + (getDeltaMovement().z));
    }

    private double move(double fr, double to, double max)
    {
        double delta = to-fr;
        return fr + Math.min(Math.abs(delta), max) * Math.signum(delta);
    }

    @Environment(EnvType.CLIENT)
    private void startClient()
    {
        EntitySurroundArc surrounder = new EntitySurroundArc(this);
        surrounder.life = 233333;
        surrounder.addMotionHandler(new MotionHandler()
        {
            @Override
            public String getID() { return "killer"; }

            @Override
            public void onStart() { }

            @Override
            public void onUpdate()
            {
                if (MagManipEntityBlock.this.isRemoved()) getTarget().discard();
            }
        });
        surrounder.setArcType(EntitySurroundArc.ArcType.THIN);
        world.addFreshEntity(surrounder);
    }
}
