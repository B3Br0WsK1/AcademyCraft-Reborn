package cn.academy.ability.vanilla.teleporter.skill;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.sounds.SoundSource;
import cn.academy.ability.AbilityContext;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.ability.vanilla.teleporter.util.TPSkillHelper;
import cn.academy.client.render.misc.TPParticleFactory;
import cn.academy.datapart.AbilityData;
import cn.academy.entity.EntityMarker;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.*;
import cn.lambdalib2.util.EntitySelectors;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import cn.lambdalib2.util.RayTraceResult;
import net.minecraft.world.phys.Vec3;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import cn.lambdalib2.util.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import static cn.lambdalib2.util.MathUtils.lerpf;
import net.fabricmc.api.EnvType;




/**
  * @author WeAthFolD, KSkun
  */
public class ShiftTeleport extends Skill
{
    public static ShiftTeleport instance = new ShiftTeleport();
    public ShiftTeleport()
    {
      super("shift_tp", 4);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void activate(ClientRuntime rt, int keyID)
    {
      activateSingleKey2(rt, keyID, p -> new STContext(p));
    }

    @Override
    public float getSkillExp(AbilityData data){
        if(AbilityContext.of(data.getEntity(), this).isEntirelyDisableBreakBlock())
          return 1f;
        else
          return data.getSkillExp(this);
    }

    public static class STContext extends Context
    {
        private static final String MSG_EXECUTE = "execute";
        public STContext(Player p)
        {
            super(p, ShiftTeleport.instance);
        }

        private float exp = ctx.getSkillExp();
        private boolean attacked = false;

        @Listener(channel=MSG_MADEALIVE, side= Side.SERVER)
        private void s_madeAlive(){
            if(!isHandValid())
                terminate();
        }

        @Listener(channel=MSG_KEYUP, side=Side.CLIENT)
        private void l_onKeyUp(){
            if(isHandValid())
                sendToServer(MSG_EXECUTE);
            else
                terminate();
        }

        @Listener(channel=MSG_KEYABORT, side=Side.CLIENT)
        private void l_onKeyAbort(){
            sendToSelf(MSG_EXECUTE, false);
            terminate();
        }


        @Listener(channel=MSG_EXECUTE, side=Side.SERVER)
        private void s_execute(){
            ItemStack stack = player.getMainHandItem();
            Block block = Block.byItem(stack.getItem());
            attacked = (stack.getItem() != Items.AIR && (stack.getItem() instanceof BlockItem ) && block != Blocks.AIR);
            if(!attacked)
            {
                terminate();
                return ;
            }
            sendToClient(MSG_EXECUTE, attacked);

            BlockItem item = (BlockItem) stack.getItem();
            RayTraceResult position = getTracePosition();
            if(ctx.consume(getOverload(exp), getConsumption(exp)))
            {
                BlockPos placePos = position.getBlockPos().relative(position.sideHit);
                net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(
                    position.hitVec, position.sideHit, position.getBlockPos(), false);
                net.minecraft.world.item.context.BlockPlaceContext placeCtx =
                    new net.minecraft.world.item.context.BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack.copy(), hit);
                if (ctx.canBreakBlock(player.level(), placePos) && placeCtx.canPlace()
                        && item.place(placeCtx).consumesAction())//Can place
                {
                }
                else//Drop as item
                {
                    ItemStack drop = stack.copy();
                    drop.setCount(1);
                    player.level().addFreshEntity(new ItemEntity(player.level(), position.hitVec.x, position.hitVec.y, position.hitVec.z, drop));
                }

                if(!player.getAbilities().instabuild) {
                    if (stack.getCount() <= 1) {
                        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    } else {
                        stack.setCount(stack.getCount() - 1);
                    }
                }

                List<Entity> list = getTargetsInLine();
                for(Entity target :list) {
                    TPSkillHelper.attack(ctx, target, getDamage(exp));
                }
                player.level().playSound(player, player.blockPosition(),
                        net.minecraft.sounds.SoundEvent.createVariableRangeEvent(Identifier.parse("academy:tp.tp_shift")),
                        SoundSource.AMBIENT, 0.5f, 1f);
                ctx.addSkillExp(getExpIncr(list.size()));
                ctx.setCooldown((int)lerpf(100, 60, exp));
            }
            terminate();
        }

        private boolean isHandValid()
        {
            ItemStack stack  = player.getMainHandItem();
            return stack.getItem() != Items.AIR && (stack.getItem() instanceof BlockItem) &&
                    Block.byItem(stack.getItem()) != Blocks.AIR;
        }

        private float getExpIncr(int attackEntities)
        {
            return (1 + attackEntities) * 0.002f;
        }

        private float getDamage(float exp)
        {
            return lerpf(15, 35, exp);
        }

        private float getRange(float exp)
        {
          return lerpf(25, 35, exp);
        }

        private float getConsumption(float exp)
        {
          return lerpf(260, 320, exp);
        }

        private float getOverload(float exp)
        {
            return lerpf(40, 30, exp);
        }

      // TODO: Some boilerplate... Clean this up in case you aren't busy
        int[] getTraceDest()
        {
            double range = getRange(exp);
            RayTraceResult result = Raytrace.traceLiving(player, range, EntitySelectors.nothing());
            if(result != null) {
                Direction dir =result.sideHit;
                return new int[]{result.getBlockPos().getX() + dir.getStepX(),
                        result.getBlockPos().getY() + dir.getStepY(),
                        result.getBlockPos().getZ() + dir.getStepZ()};
            }
            Vec3 mo = VecUtils.lookingPos(player, range);
            return new int[]{(int) mo.x, (int) mo.y, (int) mo.z};
        }

        RayTraceResult getTracePosition(){
            double range = getRange(exp);
            RayTraceResult result = Raytrace.traceLiving(player, range, EntitySelectors.nothing());
            if(result != null) {
              Direction dir = result.sideHit;
              result.hitVec = result.hitVec.add(dir.getStepX(),
                      dir.getStepY(), dir.getStepZ());
              return result;
            }
            Vec3 mo = VecUtils.lookingPos(player, range);
            return new RayTraceResult(RayTraceResult.Type.ENTITY, mo,
                    Direction.DOWN, BlockPos.containing(mo.x, mo.y, mo.z));
        }

        List<Entity> getTargetsInLine()
        {
            int[] dest = getTraceDest();
            Vec3 v0= new Vec3(player.getX(), player.getY(), player.getZ());
            Vec3 v1 = new Vec3(dest[0] + .5, dest[1] + .5, dest[2] + .5);
            AABB area = WorldUtils.minimumBounds(v0, v1);
            Predicate<Entity> pred = EntitySelectors.living().and(EntitySelectors.exclude(player)).and(new Predicate<Entity>() {

              @Override
              public boolean test(Entity entity){
                      double hw = entity.getBbWidth() / 2;
                      return VecUtils.checkLineBox(new Vec3(entity.getX() - hw, entity.getY(), entity.getZ() - hw),
                              new Vec3(entity.getX() + hw, entity.getY() + entity.getBbHeight(), entity.getZ() + hw), v0, v1) != null;
              }

            });
            return WorldUtils.getEntities(player.level(), area, pred);
        }
    }


    @Environment(EnvType.CLIENT)
    @RegClientContext(STContext.class)
    public static class STContextC extends ClientContext
    {
        private Color CRL_BLOCK_MARKER = new Color(139, 139, 139, 180);
        private Color CRL_ENTITY_MARKER = new Color(235, 81, 81, 180);

        private EntityMarker blockMarker = null;
        private List<EntityMarker> targetMarkers = null;
        private int effTicker = 0;
        private final STContext par;

        public STContextC(STContext par)
        {
            super(par);
            this.par=par;
        }

        @Listener(channel=MSG_MADEALIVE, side=Side.CLIENT)
        private void l_start(){
            targetMarkers = new ArrayList<>();
            if(isLocal()) {
                blockMarker = new EntityMarker(player.level());
                blockMarker.ignoreDepth = true;
                blockMarker.height = 1.2f;
                blockMarker.width = 1.2f;
                blockMarker.color = CRL_BLOCK_MARKER;
                blockMarker.setPosition(player.getX(), player.getY(), player.getZ());
                player.level().addFreshEntity(blockMarker);
            }
        }

        @Listener(channel=MSG_TICK, side=Side.CLIENT)
        private void l_tick(){
            if(isLocal()) {
                effTicker += 1;
                if(effTicker== 3) {
                    effTicker = 0;

                    for(Entity em : targetMarkers) {
                        em.discard();
                    }
                    targetMarkers.clear();
                    List<Entity> targetsInLine = par.getTargetsInLine();
                    for(Entity e : targetsInLine) {
                        EntityMarker em = new EntityMarker(e);
                        em.color = CRL_ENTITY_MARKER;
                        em.ignoreDepth = true;
                        player.level().addFreshEntity(em);
                        targetMarkers.add(em);
                    }
                }
                int[] dest = par.getTraceDest();
                blockMarker.setPosition(dest[0] + 0.5, dest[1], dest[2] + 0.5);
            }
        }

        @Listener(channel=MSG_TERMINATED, side=Side.CLIENT)
        private void l_terminated(){
            if(isLocal()) {
                for(Entity em : targetMarkers)
                    em.discard();
                if(blockMarker != null)
                    blockMarker.discard();
            }
        }

      @Listener(channel=STContext.MSG_EXECUTE, side=Side.CLIENT)
      private void c_end(boolean attacked){
          if(isLocal()) {
              for(Entity em : targetMarkers) em.discard();
              blockMarker.discard();
          }

          if(attacked) {
              int[] dest = par.getTraceDest();
              double dx = dest[0] + .5 - player.getX();
              double dy = dest[1] + .5 - (player.getY() - 0.5);
              double dz = dest[2] + .5 - player.getZ();
              double dist = MathUtils.length(dx, dy, dz);
              double posX = player.getX();
              double posY = player.getY() - 0.5;
              double posZ = player.getZ();
              Vec3 dv = new Vec3(dx, dy, dz).normalize();
              double move = 1;
              double x = move;
              while(x <= dist) {
                {
                  posX += dv.x * move;
                  posY += dv.y * move;
                  posZ += dv.z * move;
                  player.level().addFreshEntity(TPParticleFactory.instance.next(player.level(), new Vec3(posX, posY, posZ),
                          new Vec3(RandUtils.ranged(-.05, .05), RandUtils.ranged(-.02, .05),
                                  RandUtils.ranged(-.05, .05))));
                }
                move = RandUtils.ranged(0.6, 1);
                x += move;
              }
          }
      }
    }
}
