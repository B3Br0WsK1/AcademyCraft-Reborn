package cn.academy.ability.vanilla.meltdowner.skill;

import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.Context;
import cn.academy.client.render.particle.MdParticleFactory;
import cn.academy.client.sound.ACSounds;
import cn.academy.client.sound.FollowEntitySound;
import cn.academy.event.BlockDestroyEvent;
import cn.lambdalib2.event.EventHub;
import cn.lambdalib2.particle.Particle;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.EntitySelectors;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.RayTraceResult;
import cn.lambdalib2.util.Raytrace;
import cn.lambdalib2.util.Side;
import cn.lambdalib2.util.entityx.handlers.Rigidbody;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import static cn.lambdalib2.util.MathUtils.lerpf;
import static cn.lambdalib2.util.RandUtils.ranged;

/**
 * Base of the mine rays. Ported from Scala.
 * @author WeAthFolD, KSkun
 */
public abstract class MineRaysBase extends Skill {

    public Identifier particleTexture;
    public final String postfix;

    protected MineRaysBase(String _postfix, int atLevel) {
        super("mine_ray_" + _postfix, atLevel);
        postfix = _postfix;
    }

    public static final String MSG_PARTICLES = "particles";

    /**
     * Replacement of the 1.12 harvest levels (0 wood/gold, 1 stone, 2 iron, 3 diamond).
     */
    public static int harvestLevel(BlockState state) {
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return 3;
        if (state.is(BlockTags.NEEDS_IRON_TOOL)) return 2;
        if (state.is(BlockTags.NEEDS_STONE_TOOL)) return 1;
        return 0;
    }

    /**
     * Breaks the block as if mined with a diamond pickaxe (with the given fortune level).
     */
    public static void breakBlock(Level world, BlockPos pos, Player player, int fortune) {
        BlockState state = world.getBlockState(pos);
        SoundEvent snd = state.getSoundType().getBreakSound();
        world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, snd, SoundSource.BLOCKS, .5f, 1f);
        if (world instanceof ServerLevel sl) {
            ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
            if (fortune > 0) {
                tool.enchant(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), fortune);
            }
            Block.dropResources(state, sl, pos, world.getBlockEntity(pos), player, tool);
        }
        world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }

    public abstract static class MRContext extends Context<MineRaysBase> {

        protected int x = -1, y = -1, z = -1;
        protected float hardnessLeft = Float.MAX_VALUE;
        protected float exp = ctx.getSkillExp();

        private float overloadKeep = 0f;

        private float range, speed_l, speed_r, cp_l, cp_r, o_l, o_r, cd_l, cd_r, expincr;
        private int harvestLevel = 0;

        protected MRContext(Player p, MineRaysBase skill) {
            super(p, skill);
        }

        @Listener(channel=MSG_KEYUP, side=Side.CLIENT)
        private void l_onEnd() {
            terminate();
        }

        @Listener(channel=MSG_KEYABORT, side=Side.CLIENT)
        private void l_onAbort() {
            terminate();
        }

        @Listener(channel=MSG_MADEALIVE, side=Side.SERVER)
        private void s_onStart() {
            ctx.consume(lerpf(o_l, o_r, exp), 0);
            overloadKeep = ctx.cpData.getOverload();
        }

        @Listener(channel=MSG_TICK, side=Side.SERVER)
        private void s_onTick() {
            if (ctx.cpData.getOverload() < overloadKeep) ctx.cpData.setOverload(overloadKeep);
            if (!ctx.consume(0, lerpf(cp_l, cp_r, exp)) && !isRemote()) terminate();

            RayTraceResult result = Raytrace.traceLiving(player, range, EntitySelectors.nothing());
            if (result != null && result.typeOfHit == RayTraceResult.Type.BLOCK) {
                BlockPos pos = result.getBlockPos();
                Level world = world();
                if (pos.getX() != x || pos.getY() != y || pos.getZ() != z) {
                    BlockState is = world.getBlockState(pos);
                    if (!EventHub.BUS.post(new BlockDestroyEvent(world, player, pos)) &&
                        harvestLevel(is) <= harvestLevel) {
                        x = pos.getX();
                        y = pos.getY();
                        z = pos.getZ();
                        hardnessLeft = is.getDestroySpeed(world, pos);
                        if (hardnessLeft < 0) hardnessLeft = Float.MAX_VALUE;
                    } else {
                        x = y = z = -1;
                    }
                } else {
                    hardnessLeft -= lerpf(speed_l, speed_r, exp);
                    if (hardnessLeft <= 0) {
                        onBlockBreak(world, new BlockPos(x, y, z));
                        ctx.addSkillExp(expincr);
                        x = y = z = -1;
                    }
                    sendToClient(MSG_PARTICLES, x, y, z);
                }
            } else {
                x = y = z = -1;
            }
        }

        @Listener(channel=MSG_TERMINATED, side=Side.SERVER)
        private void s_terminated() {
            ctx.setCooldown((int) lerpf(cd_l, cd_r, exp));
        }

        protected void setRange(float _range) { range = _range; }
        protected void setHarvestLevel(int _level) { harvestLevel = _level; }
        protected void setSpeed(float l, float r) { speed_l = l; speed_r = r; }
        protected void setConsumption(float l, float r) { cp_l = l; cp_r = r; }
        protected void setOverload(float l, float r) { o_l = l; o_r = r; }
        protected void setCooldown(float l, float r) { cd_l = l; cd_r = r; }
        protected void setExpIncr(float amt) { expincr = amt; }

        protected abstract void onBlockBreak(Level world, BlockPos pos);

    }

    @Environment(EnvType.CLIENT)
    public abstract static class MRContextC extends ClientContext {

        private FollowEntitySound loopSound;
        private Entity ray;

        protected MRContextC(Context par) {
            super(par);
        }

        protected abstract Entity createRay();

        @Listener(channel=MSG_MADEALIVE, side=Side.CLIENT)
        private void c_start() {
            ray = createRay();
            player.level().addFreshEntity(ray);
            loopSound = new FollowEntitySound(player, "md.mine_loop", SoundSource.PLAYERS).setLoop().setVolume(0.3f);
            ACSounds.playClient(loopSound);
            ACSounds.playClient(player, "md.mine_" + ((MineRaysBase) skill).postfix + "_startup", SoundSource.AMBIENT, 0.4f);
        }

        @Listener(channel=MSG_TERMINATED, side=Side.CLIENT)
        private void c_end() {
            if (ray != null) ray.discard();
            if (loopSound != null) loopSound.stop();
        }

        @Listener(channel=MSG_PARTICLES, side=Side.CLIENT)
        private void c_spawnParticles(int x, int y, int z) {
            if (x == -1 && y == -1 && z == -1) return;
            int max = RandUtils.rangei(2, 3);
            for (int i = 0; i <= max; ++i) {
                double _x = x + ranged(-.2, 1.2);
                double _y = y + ranged(-.2, 1.2);
                double _z = z + ranged(-.2, 1.2);
                Particle p = MdParticleFactory.INSTANCE.next(player.level(), new Vec3(_x, _y, _z),
                    new Vec3(ranged(-.06, .06), ranged(-.06, .06), ranged(-.06, .06)));
                Identifier tex = ((MineRaysBase) skill).particleTexture;
                if (tex != null) p.texture = tex;
                p.needRigidbody = false;
                Rigidbody rb = new Rigidbody();
                rb.gravity = 0.01;
                rb.entitySel = null;
                rb.blockFil = null;
                p.addMotionHandler(rb);
                player.level().addFreshEntity(p);
            }
        }

    }

}
