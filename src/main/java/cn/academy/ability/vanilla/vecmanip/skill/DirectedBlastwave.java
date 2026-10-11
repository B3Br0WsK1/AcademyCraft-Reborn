package cn.academy.ability.vanilla.vecmanip.skill;

import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.IConsumptionProvider;
import cn.academy.ability.context.RegClientContext;
import cn.academy.ability.vanilla.vecmanip.client.effect.WaveEffect;
import cn.academy.client.render.util.AnimPresets;
import cn.academy.client.render.util.IHandRenderer;
import cn.academy.client.render.util.VanillaHandRenderer;
import cn.academy.client.sound.ACSounds;
import cn.academy.datapart.HandRenderOverrideData;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.EntitySelectors;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.RayTraceResult;
import cn.lambdalib2.util.Raytrace;
import cn.lambdalib2.util.Side;
import cn.lambdalib2.util.VecUtils;
import cn.lambdalib2.util.WorldUtils;
import cn.lambdalib2.vis.animation.presets.CompTransformAnim;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.DoubleSupplier;

import static cn.lambdalib2.util.RandUtils.RNG;
import static cn.lambdalib2.util.RandUtils.rangef;
import static cn.lambdalib2.util.RandUtils.rangei;
import static cn.lambdalib2.util.VecUtils.add;
import static cn.lambdalib2.util.VecUtils.entityHeadPos;
import static cn.lambdalib2.util.VecUtils.multiply;
import static cn.lambdalib2.util.VecUtils.setMotion;
import static cn.lambdalib2.util.VecUtils.subtract;

/**
 * Directed blastwave. Ported from Scala.
 */
public class DirectedBlastwave extends Skill {

    public static final DirectedBlastwave INSTANCE = new DirectedBlastwave();

    private DirectedBlastwave() {
        super("dir_blast", 3);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        activateSingleKey(rt, keyID, BlastwaveContext::new);
    }

    public static final String MSG_EFFECT = "effect";
    public static final String MSG_PERFORM = "perform";
    public static final String MSG_ATTACK_ENTITY = "entity";
    public static final String MSG_GENERATE_EFFECT_BLOCKS = "effect_blocks";

    public static class BlastwaveContext extends Context<DirectedBlastwave> implements IConsumptionProvider {

        static final int MIN_TICKS = 6;
        static final int MAX_ACCEPTED_TICKS = 50;
        static final int MAX_TOLERANT_TICKS = 200;
        static final int PUNCH_ANIM_TICKS = 6;

        int ticker = 0;

        boolean punched = false;
        int punchTicker = 0;

        private final float consumption = MathUtils.lerpf(160, 200, ctx.getSkillExp());
        private final float breakProb = MathUtils.lerpf(0.5f, 0.8f, ctx.getSkillExp());
        private final float breakHardness;
        private final float damage = MathUtils.lerpf(10, 25, ctx.getSkillExp());
        private final float dropRate = MathUtils.lerpf(0.4f, 0.9f, ctx.getSkillExp());
        private final int cooldown = (int) MathUtils.lerpf(80, 50, ctx.getSkillExp());

        public BlastwaveContext(Player p) {
            super(p, INSTANCE);
            float exp = ctx.getSkillExp();
            breakHardness = exp < 0.25f ? 2.9f : exp < 0.5f ? 25f : 55f;
        }

        @Listener(channel=MSG_KEYUP, side=Side.CLIENT)
        public void l_keyUp() {
            if (ticker > MIN_TICKS && ticker < MAX_ACCEPTED_TICKS) {
                sendToServer(MSG_PERFORM, ticker);
            } else {
                terminate();
            }
        }

        @Listener(channel=MSG_KEYABORT, side=Side.CLIENT)
        public void l_keyAbort() {
            terminate();
        }

        @Listener(channel=MSG_TICK, side=Side.CLIENT)
        public void l_tick() {
            if (!isLocal()) return;
            ticker += 1;
            if (ticker >= MAX_TOLERANT_TICKS) {
                terminate();
            }
            if (punched) {
                punchTicker += 1;
            }
            if (punched && punchTicker > PUNCH_ANIM_TICKS) {
                terminate();
            }
        }

        @Listener(channel=MSG_PERFORM, side=Side.SERVER)
        public void s_perform(int ticks) {
            if (tryConsume()) {
                Level world = world();
                RayTraceResult trace = Raytrace.traceLiving(player, 4, EntitySelectors.living());
                Vec3 position;
                if (trace == null || trace.typeOfHit == RayTraceResult.Type.MISS) {
                    position = add(player.position(), multiply(player.getLookAngle(), 4));
                } else if (trace.typeOfHit == RayTraceResult.Type.BLOCK) {
                    position = new Vec3(trace.getBlockPos().getX(), trace.getBlockPos().getY(), trace.getBlockPos().getZ());
                } else if (trace.typeOfHit == RayTraceResult.Type.ENTITY) {
                    position = entityHeadPos(trace.entityHit);
                } else {
                    position = trace.hitVec;
                }

                ctx.setCooldown(cooldown);
                sendToClient(MSG_PERFORM, position);

                boolean effective = false;

                // Hurt entities around
                List<Entity> entities = WorldUtils.getEntities(world,
                    position.x, position.y, position.z,
                    3, EntitySelectors.exclude(player));

                for (Entity entity : entities) {
                    ctx.attack(entity, damage);
                    knockback(entity);

                    Vec3 delta = multiply(subtract(entity.position(), player.position()).normalize(), 0.24);
                    setMotion(entity, delta);
                    entity.needsSync = true;
                    effective = true;
                }

                sendToClient(MSG_ATTACK_ENTITY, (Object) entities.toArray(new Entity[0]));

                // Destroy blocks around
                int x = (int) Math.floor(Math.round(position.x)),
                    y = (int) Math.floor(Math.round(position.y)),
                    z = (int) Math.floor(Math.round(position.z));

                for (int i = x - 3; i < x + 3; ++i) {
                    for (int j = y - 3; j < y + 3; ++j) {
                        for (int k = z - 3; k < z + 3; ++k) {
                            int dx = i - x, dy = j - y, dz = k - z;
                            int distSq = dx * dx + dy * dy + dz * dz;
                            if ((distSq <= 6) && (distSq == 0 || RNG.nextFloat() < breakProb)) {
                                BlockPos bPos = new BlockPos(i, j, k);
                                BlockState state = world.getBlockState(bPos);
                                if (state.isAir()) continue;
                                Block block = state.getBlock();
                                float hardness = state.getDestroySpeed(world, bPos);
                                if (0 <= hardness && hardness <= breakHardness && ctx.getSkillExp() == 1f) {
                                    if (ctx.canBreakBlock(world, bPos)) {
                                        ItemStack itemStack = new ItemStack(block);
                                        if (!itemStack.isEmpty())
                                            world.addFreshEntity(new ItemEntity(world, i, j, k, itemStack));
                                        world.setBlockAndUpdate(bPos, Blocks.AIR.defaultBlockState());
                                    }
                                } else if (0 <= hardness && hardness <= breakHardness && ctx.canBreakBlock(world, i, j, k)) {
                                    if (RNG.nextFloat() < dropRate && world instanceof ServerLevel sl)
                                        Block.dropResources(state, sl, bPos);

                                    world.setBlockAndUpdate(bPos, Blocks.AIR.defaultBlockState());
                                }
                            }
                        }
                    }
                }

                sendToClient(MSG_GENERATE_EFFECT_BLOCKS, position);

                ctx.addSkillExp(effective ? 0.0025f : 0.0012f);

            } else {
                terminate();
            }
        }

        @Listener(channel=MSG_ATTACK_ENTITY, side=Side.CLIENT)
        public void c_effect(Entity[] entities) {
            for (Entity e : entities) {
                if (e != null) knockback(e);
            }
        }

        @Listener(channel=MSG_PERFORM, side=Side.CLIENT)
        public void c_perform(Vec3 pos) {
            sendToSelf(MSG_EFFECT, pos);

            punched = true;
        }

        private boolean tryConsume() {
            float overload = MathUtils.lerpf(50, 30, ctx.getSkillExp());

            return ctx.consume(overload, consumption);
        }

        @Override
        public float getConsumptionHint() {
            return consumption;
        }

        private void knockback(Entity targ) {
            Vec3 delta = subtract(entityHeadPos(player), entityHeadPos(targ));
            delta = delta.normalize();
            delta = new Vec3(delta.x, delta.y - 0.4f, delta.z);
            delta = delta.normalize();

            targ.setPos(targ.getX(), targ.getY() + 0.1, targ.getZ());
            setMotion(targ, multiply(delta, -1.2f));
            targ.needsSync = true;
        }

    }

    @Environment(EnvType.CLIENT)
    @RegClientContext(BlastwaveContext.class)
    public static class BlastwaveContextC extends ClientContext {

        IHandRenderer handEffect;

        CompTransformAnim anim;

        DoubleSupplier timeProvider = null;

        public BlastwaveContextC(Context par) {
            super(par);
        }

        @Listener(channel=MSG_EFFECT, side=Side.CLIENT)
        private void effectAt(Vec3 pos) {
            ACSounds.playClient(world(), pos.x, pos.y, pos.z, "vecmanip.directed_blast", SoundSource.AMBIENT, 0.5f, 1.0f);

            WaveEffect effect = new WaveEffect(world(), rangei(2, 3), 1);
            Vec3 headPosition = VecUtils.entityHeadPos(player);
            effect.setPos(
                MathUtils.lerp(headPosition.x, pos.x, 0.7),
                MathUtils.lerp(headPosition.y, pos.y, 0.7),
                MathUtils.lerp(headPosition.z, pos.z, 0.7));
            effect.setYRot(player.getYHeadRot() + rangef(-20, 20));
            effect.setXRot(player.getXRot() + rangef(-10, 10));

            world().addFreshEntity(effect);
        }

        @Listener(channel=MSG_MADEALIVE, side=Side.CLIENT)
        public void l_handEffectStart() {
            if (!isLocal()) return;
            anim = AnimPresets.createPrepareAnim();

            double init = GameTimer.getTime();
            timeProvider = () -> Math.min(2.0, (GameTimer.getTime() - init) / 0.150);

            handEffect = partialTicks -> {
                anim.perform(timeProvider.getAsDouble());
                VanillaHandRenderer.renderHand(partialTicks, anim.target);
            };

            HandRenderOverrideData.get(player).addInterrupt(handEffect);
        }

        @Listener(channel=MSG_TERMINATED, side=Side.CLIENT)
        public void l_handEffectTerminate() {
            if (isLocal()) {
                HandRenderOverrideData.get(player).stopInterrupt(handEffect);
            }
        }

        @Listener(channel=MSG_PERFORM, side=Side.CLIENT)
        public void l_effect() {
            if (!isLocal()) return;
            double init = GameTimer.getTime();
            timeProvider = () -> (GameTimer.getTime() - init) / 0.3;

            anim = AnimPresets.createPunchAnim();
            anim.perform(0);
        }

    }

}
