package cn.academy.ability.vanilla.vecmanip.skill;

import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.ClientRuntime.IActivateHandler;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.ContextManager;
import cn.academy.ability.context.DelegateState;
import cn.academy.ability.context.KeyDelegate;
import cn.academy.ability.context.RegClientContext;
import cn.academy.ability.vanilla.vecmanip.client.effect.StormWingEffect;
import cn.academy.client.sound.ACSounds;
import cn.academy.client.sound.FollowEntitySound;
import cn.lambdalib2.input.Keys;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.EntitySelectors;
import cn.lambdalib2.util.RayTraceResult;
import cn.lambdalib2.util.Raytrace;
import cn.lambdalib2.util.Side;
import cn.lambdalib2.util.VecUtils;
import cn.lambdalib2.util.WorldUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.function.Supplier;

import static cn.lambdalib2.util.MathUtils.lerpf;
import static cn.lambdalib2.util.MathUtils.toRadians;
import static cn.lambdalib2.util.RandUtils.ranged;

/**
 * Storm wing. Ported from Scala.
 */
public class StormWing extends Skill {

    public static final StormWing INSTANCE = new StormWing();

    private StormWing() {
        super("storm_wing", 3);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        rt.addKey(keyID, new KeyDelegate() {
            @Override
            public void onKeyDown() {
                Optional<StormWingContext> cur = currentContext();
                if (cur.isPresent()) {
                    cur.get().terminate();
                } else {
                    ContextManager.instance.activate(new StormWingContext(rt.getEntity()));
                }
            }

            @Override
            public DelegateState getState() {
                return currentContext()
                    .map(ctx -> ctx.getState() == STATE_ACTIVE ? DelegateState.ACTIVE : DelegateState.CHARGE)
                    .orElse(DelegateState.IDLE);
            }

            private Optional<StormWingContext> currentContext() {
                return ContextManager.instance.findLocal(StormWingContext.class);
            }

            @Override
            public Identifier getIcon() {
                return INSTANCE.getHintIcon();
            }

            @Override
            public int createID() {
                return 0;
            }

            @Override
            public Skill getSkill() {
                return INSTANCE;
            }
        });
    }

    public static final String MSG_UPDSTATE = "upd_state";
    public static final String MSG_SYNC_STATE = "sync_state";

    public static final String KEY_GROUP = "vm_storm_wing";

    public static final int STATE_CHARGE = 0;
    public static final int STATE_ACTIVE = 1;

    public static final double ACCEL = 0.16;

    public static class StormWingContext extends Context<StormWing> {

        private Supplier<Vec3> currentDir = null;
        private boolean applying = false;
        private int keyid = -1;

        private int state = STATE_CHARGE;
        private int stateTick = 0;

        private boolean prevAllowFlying;

        private final float consumption = lerpf(40, 25, ctx.getSkillExp());
        private final float overload = lerpf(10, 7, ctx.getSkillExp());
        private final float speed = (ctx.getSkillExp() < 0.45f ? 0.7f : 1.2f) * lerpf(2, 3, ctx.getSkillExp());
        private final float expincr = 0.00005f; // per tick

        public final float chargeTime = lerpf(70, 30, ctx.getSkillExp());

        public StormWingContext(Player p) {
            super(p, INSTANCE);
        }

        @Listener(channel=MSG_MADEALIVE, side=Side.SERVER)
        private void s_makeAlive() {
            prevAllowFlying = player.getAbilities().mayfly;
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }

        @Listener(channel=MSG_KEYDOWN, side=Side.CLIENT)
        private void l_keyDown(Supplier<Vec3> dir, int _keyid) {
            if (state == STATE_ACTIVE) {
                currentDir = dir;
                keyid = _keyid;
                l_syncState();
            }
        }

        @Listener(channel=MSG_KEYUP, side=Side.CLIENT)
        private void l_keyUp(int _keyid) {
            if (currentDir != null && keyid == _keyid) {
                currentDir = null;
                l_syncState();
            }
        }

        private void l_syncState() {
            applying = currentDir != null;
            sendToServer(MSG_UPDSTATE, applying);
        }

        private static double move(double from, double to, double lim) {
            double delta = to - from;
            return from + Math.min(Math.abs(delta), lim) * Math.signum(delta);
        }

        @Listener(channel=MSG_UPDSTATE, side=Side.SERVER)
        private void s_update(boolean _applying) {
            applying = _applying;
            sendToExceptLocal(MSG_UPDSTATE, applying);
        }

        @Listener(channel=MSG_UPDSTATE, side=Side.CLIENT)
        private void c_update(boolean _applying) {
            applying = _applying;
        }

        @Listener(channel=MSG_TICK, side=Side.CLIENT)
        private void l_tick() {
            if (!isLocal()) return;
            Vec3 motion = player.getDeltaMovement();
            if (currentDir != null) {
                Vec3 moveDir = currentDir.get();

                Vec3 expectedVel = VecUtils.multiply(moveDir, speed);
                if (player.getVehicle() != null)
                    player.stopRiding();
                player.setDeltaMovement(
                    move(motion.x, expectedVel.x, ACCEL),
                    move(motion.y, expectedVel.y, ACCEL),
                    move(motion.z, expectedVel.z, ACCEL));
            } else {
                RayTraceResult res = Raytrace.perform(world(), VecUtils.add(player.position(), new Vec3(0, 0.5, 0)),
                    VecUtils.add(player.position(), new Vec3(0, -0.3, 0)),
                    EntitySelectors.nothing());
                if (res == null || res.typeOfHit == RayTraceResult.Type.MISS) {
                    player.setDeltaMovement(motion.x, motion.y + 0.078, motion.z);
                } else {
                    player.setDeltaMovement(motion.x, 0.1, motion.z); // Keep player floating on the air if near ground
                }
            }

            player.resetFallDistance();

            doConsume();

            if (state == STATE_CHARGE && stateTick > chargeTime) {
                state = STATE_ACTIVE;
                stateTick = 0;
                initKeys();
                sendToServer(MSG_SYNC_STATE);
            }
        }

        @Listener(channel=MSG_TICK, side=Side.CLIENT)
        private void c_tick() {
            stateTick += 1;
        }

        @Listener(channel=MSG_TERMINATED, side=Side.SERVER)
        private void s_terminate() {
            player.getAbilities().mayfly = prevAllowFlying;
            if (!prevAllowFlying) player.getAbilities().flying = false;
            player.onUpdateAbilities();
            ctx.setCooldown((int) lerpf(30, 10, ctx.getSkillExp()));
        }

        @Listener(channel=MSG_TICK, side=Side.SERVER)
        private void s_tick() {
            player.resetFallDistance();

            // Break blocks nearby player
            if (ctx.getSkillExp() < 0.15f) {
                int checkArea = 10;
                Level world = world();
                for (int i = 0; i < 40; ++i) {
                    BlockPos pos = new BlockPos(
                        (int) (player.getX() + ranged(-checkArea, checkArea)),
                        (int) (player.getY() + ranged(-checkArea, checkArea)),
                        (int) (player.getZ() + ranged(-checkArea, checkArea)));
                    BlockState is = world.getBlockState(pos);
                    float hardness = is.getDestroySpeed(world, pos);
                    if (!is.isAir() && 0 <= hardness && hardness <= 0.3f && ctx.canBreakBlock(world, pos)) {
                        world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());

                        SoundEvent snd = is.getSoundType().getBreakSound();
                        world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, snd, SoundSource.BLOCKS, .5f, 1f);
                    }
                }
            }

            if (!doConsume()) {
                terminate();
            }
        }

        private boolean doConsume() {
            if (state == STATE_ACTIVE) {
                ctx.addSkillExp(expincr);
                return ctx.consume(overload, consumption);
            }
            return true;
        }

        @Environment(EnvType.CLIENT)
        private void defkey(ClientRuntime rt, int idx, int key, Supplier<Vec3> dirFactory) {
            rt.addKey(KEY_GROUP, key, new KeyDelegate() {
                @Override
                public void onKeyDown() {
                    sendToSelf(MSG_KEYDOWN, dirFactory, key);
                }

                @Override
                public void onKeyUp() {
                    sendToSelf(MSG_KEYUP, key);
                }

                @Override
                public void onKeyAbort() {
                    onKeyUp();
                }

                @Override
                public Identifier getIcon() {
                    return INSTANCE.getHintIcon();
                }

                @Override
                public DelegateState getState() {
                    return applying && keyid == key ? DelegateState.ACTIVE : DelegateState.IDLE;
                }

                @Override
                public int createID() {
                    return idx;
                }

                @Override
                public Skill getSkill() {
                    return INSTANCE;
                }
            });
        }

        @Environment(EnvType.CLIENT)
        private void initKeys() {
            ClientRuntime rt = clientRuntime();

            Options settings = Minecraft.getInstance().options;
            defkey(rt, 1, Keys.fromKeyMapping(settings.keyUp), () -> worldSpace(0, 0, 1));
            defkey(rt, 2, Keys.fromKeyMapping(settings.keyDown), () -> worldSpace(0, 0, -1));
            defkey(rt, 3, Keys.fromKeyMapping(settings.keyLeft), () -> worldSpace(1, 0, 0));
            defkey(rt, 4, Keys.fromKeyMapping(settings.keyRight), () -> worldSpace(-1, 0, 0));
        }

        private Vec3 worldSpace(double x, double y, double z) {
            Vec3 moveDir = new Vec3(x, y, z);
            float yaw = toRadians(player.getYHeadRot()), pitch = toRadians(player.getXRot());
            return moveDir.xRot(-pitch).yRot(-yaw);
        }

        @Listener(channel=MSG_SYNC_STATE, side={Side.CLIENT, Side.SERVER})
        private void syncState() {
            this.state = STATE_ACTIVE;

            if (ctx.getSkillExp() == 1.0f) {
                for (Entity ent : WorldUtils.getEntities(player, 6, EntitySelectors.everything())) {
                    double modifier = ranged(0.9, 1.2);

                    Vec3 delta = VecUtils.subtract(VecUtils.entityHeadPos(ent), player.position()).scale(modifier);

                    Vec3 mv = VecUtils.multiply(delta.normalize(), ranged(0.5f, 1.0f));
                    ent.setDeltaMovement(mv);
                    ent.needsSync = true;
                }
            }

            if (!isRemote()) {
                sendToExceptLocal(MSG_SYNC_STATE);
            }
        }

        public int getState() {
            return state;
        }

        public int getStateTick() {
            return stateTick;
        }

        public boolean isApplying() {
            return applying;
        }

    }

    @Environment(EnvType.CLIENT)
    @RegClientContext(StormWingContext.class)
    public static class StormWingContextC extends ClientContext {

        private IActivateHandler activateHandler;

        private FollowEntitySound loopSound;

        public StormWingContextC(Context par) {
            super(par);
        }

        @Listener(channel=MSG_MADEALIVE, side=Side.CLIENT)
        private void l_makeAlive() {
            if (isLocal()) {
                activateHandler = new IActivateHandler() {
                    @Override
                    public boolean handles(Player player) {
                        return true;
                    }

                    @Override
                    public String getHint() {
                        return IActivateHandler.ENDSPECIAL;
                    }

                    @Override
                    public void onKeyDown(Player player) {
                        terminate();
                    }
                };
                clientRuntime().addActivateHandler(activateHandler);
            }
        }

        @Listener(channel=MSG_TERMINATED, side=Side.CLIENT)
        private void l_terminate() {
            if (isLocal()) {
                clientRuntime().clearKeys(KEY_GROUP);
                clientRuntime().removeActiveHandler(activateHandler);
            }
        }

        @Listener(channel=MSG_MADEALIVE, side=Side.CLIENT)
        private void c_makealive() {
            world().addFreshEntity(new StormWingEffect((StormWingContext) parent));

            loopSound = new FollowEntitySound(player, "vecmanip.storm_wing", SoundSource.AMBIENT).setLoop();
            ACSounds.playClient(loopSound);
        }

        @Listener(channel=MSG_TERMINATED, side=Side.CLIENT)
        private void c_terminate() {
            if (loopSound != null) loopSound.stop();
        }

        @Listener(channel=MSG_TICK, side=Side.CLIENT)
        private void c_tick() {
            BlockParticleOption dust = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState());
            for (int i = 0; i < 12; ++i) { // Particles that surround the player
                double theta = ranged(0, Math.PI * 2);
                double phi = ranged(-Math.PI, Math.PI);
                double r = ranged(3, 8);

                double rzx = r * Math.sin(phi);
                double cth = Math.cos(theta), sth = Math.sin(theta);
                double dx = rzx * cth, dy = r * Math.cos(phi), dz = rzx * sth;

                world().addParticle(dust,
                    player.getX() + dx, player.getY() + dy, player.getZ() + dz,
                    sth * 0.7f, ranged(-0.01f, 0.05f), -cth * 0.7f);
            }
        }

    }

}
