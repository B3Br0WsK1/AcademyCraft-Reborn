package cn.academy.ability.vanilla.vecmanip.skill;

import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.DelegateState;
import cn.academy.ability.context.IConsumptionProvider;
import cn.academy.ability.context.IStateProvider;
import cn.academy.ability.context.RegClientContext;
import cn.academy.ability.vanilla.generic.client.effect.SmokeEffect;
import cn.academy.ability.vanilla.util.HandlerLifePeroidEvent;
import cn.academy.client.sound.ACSounds;
import cn.academy.util.Plotter;
import cn.lambdalib2.event.EventHub;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.EntitySelectors;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.Side;
import cn.lambdalib2.util.WorldUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static cn.lambdalib2.util.MathUtils.lerpf;
import static cn.lambdalib2.util.RandUtils.nextDouble;
import static cn.lambdalib2.util.RandUtils.nextFloat;
import static cn.lambdalib2.util.RandUtils.ranged;
import static cn.lambdalib2.util.RandUtils.rangei;
import static cn.lambdalib2.util.VecUtils.multiply;

/**
 * Groundshock. Ported from Scala.
 */
public class Groundshock extends Skill {

    public static final Groundshock INSTANCE = new Groundshock();

    private Groundshock() {
        super("ground_shock", 1);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        activateSingleKey(rt, keyID, GroundshockContext::new);
    }

    public static final String MSG_PERFORM = "perform";

    public static class GroundshockContext extends Context<Groundshock> implements IConsumptionProvider, IStateProvider {

        public int localTick = 0;

        private final double initEnergy = lerpf(60, 120, ctx.getSkillExp());
        private final float damage = lerpf(4, 6, ctx.getSkillExp());
        private final float consumption = lerpf(80, 150, ctx.getSkillExp());
        private final float overload = lerpf(15, 10, ctx.getSkillExp());
        private final int maxIter = (int) lerpf(10, 25, ctx.getSkillExp());
        private final int cooldown = (int) lerpf(80, 40, ctx.getSkillExp());
        private final float dropRate = lerpf(0.3f, 1.0f, ctx.getSkillExp());
        // y speed given to mobs.
        private final float ySpeed = RandUtils.rangef(0.6f, 0.9f) * lerpf(0.8f, 1.3f, ctx.getSkillExp());
        private final double groundBreakProb = 0.3;

        private double energy;

        public GroundshockContext(Player p) {
            super(p, INSTANCE);
        }

        @Listener(channel=MSG_TICK, side=Side.CLIENT)
        public void l_tick() {
            localTick += 1;

            float pitchDelta;
            if (localTick < 4) pitchDelta = localTick / 4.0f;
            else if (localTick <= 20) pitchDelta = 1.0f;
            else if (localTick <= 25) pitchDelta = 1.0f - (localTick - 20) / 5.0f;
            else pitchDelta = 0.0f;

            player.setXRot(player.getXRot() - pitchDelta * 0.2f);
        }

        @Listener(channel=MSG_KEYUP, side=Side.CLIENT)
        public void l_keyUp() {
            if (localTick >= 5) {
                sendToServer(MSG_PERFORM);
            } else {
                terminate();
            }
        }

        @Listener(channel=MSG_KEYABORT, side=Side.CLIENT)
        public void l_keyAbort() {
            terminate();
        }

        private void breakWithForce(int x, int y, int z, boolean drop) {
            Level world = world();
            BlockPos blockPos = new BlockPos(x, y, z);
            BlockState state = world.getBlockState(blockPos);

            if (ctx.canBreakBlock(world, x, y, z)) {
                float hardnessEnergy = state.getDestroySpeed(world, blockPos);
                if (hardnessEnergy >= 0) {
                    if (energy >= hardnessEnergy && !state.is(Blocks.FARMLAND) && state.getFluidState().isEmpty()) {
                        energy -= hardnessEnergy;

                        if (drop && RandUtils.nextFloat() < dropRate && world instanceof ServerLevel sl) {
                            Block.dropResources(state, sl, blockPos);
                        }

                        world.setBlockAndUpdate(blockPos, Blocks.AIR.defaultBlockState());
                        world.playSound(null, x + 0.5, y + 0.5, z + 0.5, SoundEvents.ANVIL_DESTROY, SoundSource.AMBIENT, .5f, 1f);
                    }
                }
            }
        }

        private record Delta(Vec3 vec, double prob) {}

        @Listener(channel=MSG_PERFORM, side=Side.SERVER)
        public void s_perform() {
            Level world = world();
            if (player.onGround() && consume()) {
                Vec3 planeLook = player.getLookAngle().normalize();

                Plotter plotter = new Plotter((int) Math.floor(player.getX()), (int) Math.floor(player.getY()) - 1,
                    (int) Math.floor(player.getZ()), planeLook.x, 0, planeLook.z);

                int iter = maxIter;

                energy = initEnergy;
                Set<BlockPos> dejavu_blocks = new HashSet<>();
                Set<Entity> dejavu_ent = new HashSet<>();

                // NB: The original discarded the result of rotateYaw(90), so `rot` is the look vector itself.
                Vec3 rot = planeLook;

                List<Delta> deltas = List.of(new Delta(Vec3.ZERO, 1.0),
                    new Delta(rot, 0.7), new Delta(multiply(rot, -1), 0.7),
                    new Delta(multiply(rot, 2), 0.3), new Delta(multiply(rot, -2), 0.3));

                Predicate<Entity> selector = EntitySelectors.living().and(EntitySelectors.exclude(player));

                while (energy > 0 && iter > 0) {
                    int[] next = plotter.next();
                    int x = next[0], y = next[1], z = next[2];

                    iter -= 1;

                    for (Delta d : deltas) {
                        Vec3 delta = d.vec();
                        BlockPos pos = new BlockPos((int) Math.floor(x + delta.x), (int) Math.floor(y + delta.y),
                            (int) Math.floor(z + delta.z));
                        BlockState is = world.getBlockState(pos);

                        if (RandUtils.nextDouble() < d.prob()) {
                            if (!is.isAir() && !dejavu_blocks.contains(pos)) {
                                dejavu_blocks.add(pos);

                                if (is.is(Blocks.STONE)) {
                                    world.setBlockAndUpdate(pos, Blocks.COBBLESTONE.defaultBlockState());
                                    energy -= 0.4;
                                } else if (is.is(Blocks.GRASS_BLOCK)) {
                                    world.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
                                    energy -= 0.2;
                                } else if (is.is(Blocks.FARMLAND)) {
                                    energy -= 0.1;
                                } else {
                                    energy -= 0.5;
                                }

                                if (RandUtils.nextDouble() < groundBreakProb) {
                                    breakWithForce(x, y, z, false);
                                }

                                AABB aabb = new AABB(pos.getX() - 0.2, pos.getY() - 0.2, pos.getZ() - 0.2,
                                    pos.getX() + 1.4, pos.getY() + 2.2, pos.getZ() + 1.4);
                                for (Entity entity : WorldUtils.getEntities(world, aabb, selector)) {
                                    if (dejavu_ent.add(entity)) {
                                        energy -= 1;
                                        ctx.attack(entity, damage);

                                        Vec3 m = entity.getDeltaMovement();
                                        entity.setDeltaMovement(m.x, ySpeed, m.z);
                                        entity.needsSync = true;
                                        ctx.addSkillExp(0.002f);
                                    }
                                }
                            }
                        }

                        for (int dy = 1; dy <= 3; ++dy) breakWithForce(x, y + dy, z, false);
                    }
                }

                energy = Double.MAX_VALUE;
                if (ctx.getSkillExp() == 1) {
                    int x0 = (int) player.getX(), y0 = (int) player.getY(), z0 = (int) player.getZ();
                    for (int x = x0 - 5; x < x0 + 5; ++x) {
                        for (int y = y0 - 1; y < y0 + 1; ++y) {
                            for (int z = z0 - 5; z < z0 + 5; ++z) {
                                BlockPos pos = new BlockPos(x, y, z);
                                float hardness = world.getBlockState(pos).getDestroySpeed(world, pos);
                                if (hardness <= 0.6) {
                                    breakWithForce(x, y, z, true);
                                }
                            }
                        }
                    }
                }

                ctx.addSkillExp(0.001f);
                ctx.setCooldown(cooldown);

                int[] affected = new int[dejavu_blocks.size() * 3];
                int i = 0;
                for (BlockPos v : dejavu_blocks) {
                    affected[i++] = v.getX();
                    affected[i++] = v.getY();
                    affected[i++] = v.getZ();
                }
                sendToClient(MSG_PERFORM, (Object) affected);
            }
            terminate();
        }

        @Override
        public float getConsumptionHint() {
            return consumption;
        }

        @Override
        public DelegateState getState() {
            return localTick < 5 ? DelegateState.CHARGE : DelegateState.ACTIVE;
        }

        boolean consume() {
            return ctx.consume(overload, consumption);
        }

    }

    @Environment(EnvType.CLIENT)
    @RegClientContext(GroundshockContext.class)
    public static class GroundshockContextC extends ClientContext {

        public GroundshockContextC(Context par) {
            super(par);
        }

        @Listener(channel=MSG_PERFORM, side=Side.CLIENT)
        public void c_perform(int[] affectedBlocks) {
            if (isLocal()) {
                ((GroundshockContext) parent).consume();

                // Starts a coroutine that make player's look direction slash down.
                EventHub.BUS.register(new HandlerLifePeroidEvent(4) {
                    @Override
                    public boolean onTick() {
                        player.setXRot(player.getXRot() + 3.4f);
                        return true;
                    }

                    @Override
                    public void onDeath() {
                        EventHub.BUS.unregister(this);
                    }
                });
            }

            ACSounds.playClient(player, "vecmanip.groundshock", SoundSource.AMBIENT, 2);

            Level world = world();
            for (int idx = 0; idx + 2 < affectedBlocks.length; idx += 3) {
                BlockPos pt = new BlockPos(affectedBlocks[idx], affectedBlocks[idx + 1], affectedBlocks[idx + 2]);
                BlockState is = world.getBlockState(pt);
                if (!is.isAir()) {
                    BlockParticleOption crack = new BlockParticleOption(ParticleTypes.BLOCK, is);
                    int n = rangei(4, 8);
                    for (int i = 0; i < n; ++i) {
                        world.addParticle(crack,
                            pt.getX() + nextDouble(), pt.getY() + 1 + nextDouble() * 0.5 + 0.2, pt.getZ() + nextDouble(),
                            ranged(-0.2, 0.2), 0.1 + nextDouble() * 0.2, ranged(-0.2, 0.2));
                    }
                }

                if (nextFloat() < 0.5f) {
                    SmokeEffect eff = new SmokeEffect(world);
                    eff.setPos(pt.getX() + 0.5 + ranged(-.3, .3), pt.getY() + 1 + ranged(0, 0.2), pt.getZ() + 0.5 + ranged(-.3, .3));
                    eff.setDeltaMovement(ranged(-.03, .03), ranged(.03, .06), ranged(-.03, .03));
                    world.addFreshEntity(eff);
                }
            }
        }

    }

}
