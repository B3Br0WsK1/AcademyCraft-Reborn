package cn.academy.ability.vanilla.electromaster.skill;

import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.client.render.util.ACRenderingHelper;
import cn.academy.client.render.util.ArcPatterns;
import cn.academy.client.sound.ACSounds;
import cn.academy.client.sound.FollowEntitySound;
import cn.academy.entity.EntityArc;
import cn.academy.entity.EntitySurroundArc;
import cn.academy.entity.EntitySurroundArc.ArcType;
import cn.academy.support.EnergyBlockHelper;
import cn.academy.support.EnergyItemHelper;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.RayTraceResult;
import cn.lambdalib2.util.Raytrace;
import cn.lambdalib2.util.Side;
import cn.lambdalib2.util.VecUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import static cn.lambdalib2.util.MathUtils.lerpf;

/**
 * Current Charging. Ported from Scala.
 * @author WeAthFolD, KSkun
 */
public class CurrentCharging extends Skill {

    public static final CurrentCharging INSTANCE = new CurrentCharging();

    private CurrentCharging() {
        super("charging", 1);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void activate(ClientRuntime rt, int keyid) {
        activateSingleKey(rt, keyid, ChargingContext::new);
    }

    public static final String MSG_EFFECT_START = "effect_start";
    public static final String MSG_EFFECT_END = "effect_end";

    public static class ChargingContext extends Context<CurrentCharging> {

        static float getChargingSpeed(float exp) {
            return (float) Math.floor(lerpf(15, 35, exp));
        }

        static float getExpIncr(boolean effective) {
            return effective ? 0.0001f : 0.00003f;
        }

        static float getConsumption(float exp) {
            return lerpf(3, 7, exp);
        }

        static float getOverload(float exp) {
            return lerpf(65, 48, exp);
        }

        private float overload = 0f;

        final double distance = 15.0d;
        private final float exp = ctx.getSkillExp();
        private final boolean isItem;

        public ChargingContext(Player p) {
            super(p, INSTANCE);
            boolean item = false;
            for (ItemStack s : java.util.List.of(ctx.player.getMainHandItem(), ctx.player.getOffhandItem())) {
                if (!s.isEmpty()) item = true;
            }
            isItem = item;
        }

        @Listener(channel=MSG_MADEALIVE, side=Side.SERVER)
        private void s_onStart() {
            ctx.consume(getOverload(exp), 0);
            overload = ctx.cpData.getOverload();
        }

        @Listener(channel=MSG_KEYDOWN, side=Side.CLIENT)
        private void l_onStart() {
            sendToServer(MSG_EFFECT_START, isItem);
        }

        @Listener(channel=MSG_EFFECT_START, side=Side.SERVER)
        private void s_onEffectStart(boolean isItem) {
            sendToClient(MSG_EFFECT_START, isItem);
        }

        @Listener(channel=MSG_EFFECT_END, side=Side.SERVER)
        private void s_onEffectEnd(boolean isItem) {
            sendToClient(MSG_EFFECT_END, isItem);
            terminate();
        }

        @Listener(channel=MSG_TICK, side=Side.SERVER)
        private void s_onTick() {
            if (ctx.cpData.getOverload() < overload) ctx.cpData.setOverload(overload);
            if (!isItem) {
                // Perform raytrace
                RayTraceResult pos = Raytrace.traceLiving(player, distance);

                boolean good = false;
                if (pos != null && pos.typeOfHit == RayTraceResult.Type.BLOCK) {
                    BlockEntity tile = player.level().getBlockEntity(pos.getBlockPos());
                    if (EnergyBlockHelper.isSupported(tile)) {
                        good = true;

                        float charge = getChargingSpeed(exp);
                        EnergyBlockHelper.charge(tile, charge, true);
                    }
                }

                ctx.addSkillExp(getExpIncr(good));
                if (!ctx.consume(0, getConsumption(exp))) {
                    sendToClient(MSG_EFFECT_END, isItem);
                    terminate();
                }
            } else {
                ItemStack stack = player.getMainHandItem();
                float cp = getConsumption(exp);

                if (!stack.isEmpty() && ctx.consume(0, cp)) {
                    float amt = getChargingSpeed(exp);

                    boolean good = EnergyItemHelper.isSupported(stack);
                    if (good)
                        EnergyItemHelper.charge(stack, amt, false);

                    ctx.addSkillExp(getExpIncr(good));
                } else {
                    sendToClient(MSG_EFFECT_END, isItem);
                    terminate();
                }
            }
        }

        @Listener(channel=MSG_KEYUP, side=Side.CLIENT)
        private void l_onEnd() {
            sendToServer(MSG_EFFECT_END, isItem);
        }

        @Listener(channel=MSG_KEYABORT, side=Side.CLIENT)
        private void l_onAbort() {
            sendToServer(MSG_EFFECT_END, isItem);
        }

    }

    @Environment(EnvType.CLIENT)
    @RegClientContext(ChargingContext.class)
    public static class ChargingContextC extends ClientContext {

        EntityArc arc;
        EntitySurroundArc surround;
        FollowEntitySound sound;
        boolean isItem = false;

        public ChargingContextC(Context par) {
            super(par);
        }

        private ChargingContext par() {
            return (ChargingContext) parent;
        }

        @Listener(channel=MSG_EFFECT_START, side=Side.CLIENT)
        private void c_startEffects(boolean isItem) {
            if (!isItem) {
                arc = new EntityArc(player, ArcPatterns.chargingArc);
                arc.lengthFixed = false;
                arc.hideWiggle = 0.8;
                arc.showWiggle = 0.2;
                arc.texWiggle = 0.8;
                player.level().addFreshEntity(arc);

                surround = new EntitySurroundArc(player.level(), player.getX(), player.getY(), player.getZ(), 1, 1)
                    .setArcType(ArcType.NORMAL).setLife(100000);
                player.level().addFreshEntity(surround);

                sound = new FollowEntitySound(player, "em.charge_loop", SoundSource.AMBIENT).setLoop().setVolume(0.3f);
                ACSounds.playClient(sound);
            } else {
                sound = new FollowEntitySound(player, "em.charge_loop", SoundSource.AMBIENT).setLoop().setVolume(0.3f);
                ACSounds.playClient(sound);
                surround = new EntitySurroundArc(player);
                surround.setArcType(ArcType.THIN);
                surround.setLife(100000);
                player.level().addFreshEntity(surround);
            }

            this.isItem = isItem;
        }

        @Listener(channel=MSG_TICK, side=Side.CLIENT)
        private void c_updateEffects() {
            if (isItem)
                return;
            // Perform raytrace
            RayTraceResult pos = Raytrace.traceLiving(player, par().distance);

            boolean good = false;
            if (pos != null && pos.typeOfHit == RayTraceResult.Type.BLOCK) {
                BlockEntity tile = player.level().getBlockEntity(pos.getBlockPos());
                if (EnergyBlockHelper.isSupported(tile)) {
                    good = true;
                }
            }

            double x, y, z;
            if (pos != null && pos.typeOfHit != RayTraceResult.Type.MISS) {
                x = pos.hitVec.x;
                y = pos.hitVec.y;
                z = pos.hitVec.z;
                if (pos.typeOfHit == RayTraceResult.Type.ENTITY) {
                    y += pos.entityHit.getEyeHeight();
                }
            } else {
                Vec3 mo = VecUtils.add(player.position(), VecUtils.multiply(player.getLookAngle(), par().distance));
                x = mo.x;
                y = mo.y;
                z = mo.z;
            }
            if (arc != null)
                arc.setFromTo(player.getX(), player.getY() + ACRenderingHelper.getHeightFix(player), player.getZ(), x, y, z);

            if (surround != null) {
                if (good) {
                    surround.updatePos(pos.getBlockPos().getX() + 0.5, pos.getBlockPos().getY(), pos.getBlockPos().getZ() + 0.5);
                    surround.draw = true;
                } else {
                    surround.draw = false;
                }
            }
        }

        @Listener(channel=MSG_EFFECT_END, side=Side.CLIENT)
        private void c_endEffects(boolean isItem) {
            if (surround != null) surround.setDead();
            if (arc != null) arc.setDead();
            if (sound != null) sound.stop();
        }

    }

}
