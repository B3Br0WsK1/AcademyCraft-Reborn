package cn.academy.ability.vanilla.electromaster.skill;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.ability.vanilla.electromaster.CatElectromaster;
import cn.academy.client.sound.ACSounds;
import cn.academy.client.sound.FollowEntitySound;
import cn.academy.entity.MagManipEntityBlock;
import cn.lambdalib2.multiblock.BlockMulti;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import cn.lambdalib2.util.RayTraceResult;
import net.minecraft.world.phys.Vec3;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import static cn.lambdalib2.util.VecUtils.*;
import net.fabricmc.api.EnvType;



/**
 * Author: WeAthFold
 * Edited by Paindar at 2018.11.24:
 *   Rewrite it with Java.
 */
public class MagManip extends Skill
{
    public static MagManip INSTANCE = new MagManip();
    public MagManip()
    {
        super("mag_manip", 2);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void activate(ClientRuntime rt, int keyid)
    {
        activateSingleKey2(rt, keyid, MagManipContext::new);
    }

    public static boolean accepts(Player player, Block block)
    {
        if(block instanceof BlockMulti)
            return false;
        else if (block instanceof DoorBlock)
            return false;
        else
            return CatElectromaster.isMetalBlock(block);
    }

    public static class MagManipContext extends Context
    {
        public MagManipContext(Player _player)
        {
            super(_player, MagManip.INSTANCE);
        }

        enum State
        {
            StateMoving, StateCharging
        }

        final static String MSG_PERFORM = "perform";

        final static String MSG_SYNC_ENTITY_REQ = "req_sync_ent";
        final static String MSG_SYNC_ENTITY_RSP = "rsp_sync_ent";


        private float consumption = MathUtils.lerpf(140, 270, ctx.getSkillExp());
        private float overload = MathUtils.lerpf(35, 20, ctx.getSkillExp());
        private int cooldown = (int) MathUtils.lerpf(60, 40, ctx.getSkillExp());
        private float damage = MathUtils.lerpf(8, 15, ctx.getSkillExp());
        private float speed = MathUtils.lerpf(0.5f, 1.0f, ctx.getSkillExp());

        public State state = State.StateMoving;
        MagManipEntityBlock entity = null;
        boolean performed = false;

        int _entityId = -1;

        @Listener(channel = MSG_KEYUP, side = Side.CLIENT)
        public void l_keyUp()
        {
            sendToServer(MSG_PERFORM);
        }

        @Listener(channel = MSG_KEYABORT, side = Side.CLIENT)
        public void l_keyAbort()
        {
            terminate();
        }

        @Listener(channel = MSG_MADEALIVE, side = Side.SERVER)
        public void s_makeAlive()
        {
            ItemStack stack = player.getMainHandItem();
            Block heldBlock = Block.byItem(stack.getItem());
            if( heldBlock != Blocks.AIR && MagManip.accepts(player, heldBlock))
            {
                if (!player.getAbilities().instabuild)
                {
                    stack.setCount(stack.getCount() - 1);
                }
                entity = new MagManipEntityBlock(player, 10);
                entity.setBlock(heldBlock);
                Vec3 hPos = entityHeadPos(player);
                entity.setPosition(hPos.x, hPos.y, hPos.z);
                world().addFreshEntity(entity);
                updateMoveTo();
            }
            else
            {
                RayTraceResult trace = Raytrace.traceLiving(player, 10, EntitySelectors.nothing(), (world, x, y, z, block1) -> accepts(player, block1));
                if (trace != null && trace.typeOfHit == RayTraceResult.Type.BLOCK)
                {
                    BlockPos pos = trace.getBlockPos();
                    BlockState ibs = world().getBlockState(pos);
                    Block block = ibs.getBlock();
                    int x = pos.getX(), y = pos.getY(), z = pos.getZ();
                    if (block instanceof DoorBlock) // FIXME: Dirty hack
                    {
                        BlockPos bPos = new BlockPos(x, y - 1, z);
                        if (world().getBlockState(bPos).getBlock() == Blocks.IRON_DOOR)
                            world().removeBlock(bPos, false);
                        else
                            world().removeBlock(pos, false);
                    }
                    else
                        world().removeBlock(pos, false);
                    entity = new MagManipEntityBlock(player, 10);
                    entity.setBlock(block);
                    entity.setPosition((int) (x + 0.5), (int) (y + 0.5), (int) (z + 0.5));
                    world().addFreshEntity(entity);
                    updateMoveTo();
                }
                else
                    terminate();
            }
        }

        @Listener(channel = MSG_MADEALIVE, side = Side.CLIENT)
        private void c_madeAlive() {
            // FIXME: If we currently sync entity in madeAlive(), the message will be ignored
            //  because at that time client is not yet alive.
            //  To make that work we need some kind of message queue, to save msgs from server and flushes after the context becomes alive.
            sendToServer(MSG_SYNC_ENTITY_REQ);
        }

        @Listener(channel = MSG_SYNC_ENTITY_REQ, side = Side.SERVER)
        private void s_onSyncEntityReq() {
            sendToClient(MSG_SYNC_ENTITY_RSP, entity.getId());
        }

        @Listener(channel=MSG_SYNC_ENTITY_RSP, side=Side.CLIENT)
        private void c_syncEntity(int id) {
            _entityId = id;
        }

        @Listener(channel=MSG_TICK, side=Side.SERVER)
        private void s_tick() {
            updateMoveTo();
        }

        @Listener(channel=MSG_TICK, side=Side.CLIENT)
        private void c_tick() {
            if (entity == null) {
                Entity e = world().getEntity(_entityId);
                if (e instanceof MagManipEntityBlock) {
                    entity = ((MagManipEntityBlock) e);
                    _entityId = 0;
                }
            }

            if (entity != null) {
                updateMoveTo();
            }
        }

        @Listener(channel=MSG_PERFORM, side=Side.SERVER)
        public void s_perform()
        {
            performed=true;
            entity.actionType = MagManipEntityBlock.ActNothing;
            entity.setPlaceFromServer(true);

            double distsq = player.distanceToSqr(entity);
            if(distsq < 25 && ctx.consume(overload, consumption))
            {
                Vec3 pos = Raytrace.getLookingPos(player, 20).getLeft();
                Vec3 delta = subtract(pos, entity.position());
                Vec3 velocity = multiply(delta.normalize(), speed);
                setMotion(entity, velocity);

                ctx.setCooldown(cooldown);
                ctx.addSkillExp(0.005F);

                sendToClient(MSG_PERFORM, entity, velocity);
            }

            terminate();
        }

        @Listener(channel = MSG_TERMINATED, side=Side.SERVER)
        public void s_terminate()
        {
            if(!performed && entity !=null)
            {
                entity.actionType = MagManipEntityBlock.ActNothing;
                entity.setPlaceFromServer(true);
            }
        }

        private void updateMoveTo()
        {
            Vec3 origin = subtract(entityHeadPos(player),  new Vec3(0, 0.1, 0));
            Vec3 look = player.getLookAngle();

            Vec3 look2 = new Vec3(look.x , 0, look.z)
                    .normalize().xRot((float) (Math.PI/2));
            Vec3 pos = add(origin, multiply(player.getLookAngle(), 2.0));
            entity.setMoveTo(pos.x, pos.y, pos.z);
        }

        /*
        private def wrap(args: Any*) = args.map(_.asInstanceOf[AnyRef])

  override def getConsumptionHint: Float = consumption
         */
    }

    @Environment(EnvType.CLIENT)
    @RegClientContext(MagManipContext.class)
    public static class MagManipContextC extends ClientContext
    {
        FollowEntitySound loopSound;

        public MagManipContextC(MagManipContext par)
        {
            super(par);
            loopSound = new FollowEntitySound(par.player, "em.lf_loop", SoundSource.AMBIENT).setLoop();
        }

        @Listener(channel=MSG_MADEALIVE, side=Side.CLIENT)
        public void c_makeAlive()
        {
            ACSounds.playClient(loopSound);
        }

        @Listener(channel=MSG_TERMINATED, side=Side.CLIENT)
        public void c_terminate()
        {
            loopSound.stop();
        }

        @Listener(channel=MagManipContext.MSG_PERFORM, side=Side.CLIENT)
        public void c_perform(MagManipEntityBlock entity, Vec3 velocity)
        {
            entity.actionType = MagManipEntityBlock.ActNothing;
            entity.placeWhenCollide = true;
            VecUtils.setMotion(entity, velocity);

            ((MagManipContext) this.parent).entity = null;

            ACSounds.playClient(player, "em.mag_manip",  SoundSource.AMBIENT, 1.0f);
        }
    }
}
