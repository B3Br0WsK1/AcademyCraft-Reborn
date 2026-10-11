package cn.academy.ability.vanilla.teleporter.skill;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.ClientRuntime.ActivateHandlers;
import cn.academy.ability.context.ClientRuntime.IActivateHandler;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.ContextManager;
import cn.academy.ability.context.KeyDelegate;
import cn.academy.event.ability.FlushControlEvent;
import cn.academy.Resources;
import cn.academy.client.sound.ACSounds;
import cn.academy.entity.EntityTPMarking;
import cn.academy.ability.vanilla.teleporter.util.GravityCancellor;
import cn.academy.ability.vanilla.teleporter.util.TPSkillHelper;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.EntitySelectors;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.Raytrace;
import cn.lambdalib2.util.VecUtils;
import com.google.common.base.Preconditions;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import cn.lambdalib2.util.RayTraceResult;
import net.minecraft.world.phys.Vec3;
import cn.lambdalib2.event.EventHub;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import java.util.Optional;
import static cn.lambdalib2.util.MathUtils.lerpf;
import net.fabricmc.api.EnvType;





/**
 * @author WeAthFolD
 */
public class Flashing extends Skill {

    public static final Flashing instance = new Flashing();

    private static final String
            MSG_PERFORM = "perform",
            KEY_GROUP = "TP_Flashing";

    private Flashing() {
        super("flashing", 5);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void activate(ClientRuntime rt, int keyID) {
        rt.addKey(keyID, new KeyDelegate() {
            @Override
            public void onKeyDown() {
                Optional<MainContext> opt = ContextManager.instance.find(MainContext.class);
                if (!opt.isPresent()) {
                    ContextManager.instance.activate(new MainContext(getPlayer()));
                } else {
                    opt.get().terminate();
                }

                EventHub.BUS.post(new FlushControlEvent());
            }

            @Override
            public Identifier getIcon() {
                return instance.getHintIcon();
            }

            @Override
            public int createID() {
                return 0;
            }

            public Skill getSkill() {
                return instance;
            }
        });
    }

    private static final Vec3[] dirs = new Vec3[] {
            null,
            new Vec3(0, 0, -1),
            new Vec3(0, 0, 1),
            new Vec3(1, 0, 0),
            new Vec3(-1, 0, 0)
    };

    public static class MainContext extends Context {

        int performingKey = -1;

        @Environment(EnvType.CLIENT)
        EntityTPMarking marking;

        @Environment(EnvType.CLIENT)
        GravityCancellor cancellor;

        @Environment(EnvType.CLIENT)
        IActivateHandler activateHandler;

        final float exp, consumption;
        final float overload_start, consumption_start;
        final int cooldown_time;
        float overloadKeep;
        final int max_time;
        int ticks = 0;

        public MainContext(Player player) {
            super(player, instance);

            exp = ctx.getSkillExp();
            consumption = lerpf(13, 6, exp);
            overload_start = lerpf(250, 180, exp);
            consumption_start = lerpf(80, 60, exp);
            max_time = (int) lerpf(60, 150, exp);
            cooldown_time = (int) lerpf(900, 400, exp);
        }

        @Listener(channel=Context.MSG_MADEALIVE, side=Side.SERVER)
        void serverMadeAlive() {
            if(!ctx.consume(overload_start, consumption_start)) terminate(); else {
                overloadKeep = ctx.cpData.getOverload();
            }
        }

        @Environment(EnvType.CLIENT)
        @Listener(channel=Context.MSG_MADEALIVE, side=Side.CLIENT)
        void localMakeAlive() {
            if (isLocal()) {
                activateHandler = ActivateHandlers.terminatesContext(this);
                clientRuntime().addActivateHandler(activateHandler);

                final String[] strs = new String[] { null, "a", "d", "w", "s"};
                Minecraft mc = Minecraft.getInstance();
                net.minecraft.client.Options settings = mc.options;
                final int[] keys = new int[] {
                    -1,
                    cn.lambdalib2.input.Keys.fromKeyMapping(settings.keyLeft),
                    cn.lambdalib2.input.Keys.fromKeyMapping(settings.keyRight),
                    cn.lambdalib2.input.Keys.fromKeyMapping(settings.keyUp),
                    cn.lambdalib2.input.Keys.fromKeyMapping(settings.keyDown)
                };
                for (int i = 0; i < 4; ++i) {
                    final int localid = i + 1;
                    clientRuntime().addKey(KEY_GROUP, keys[localid], new KeyDelegate() {
                        @Override
                        public void onKeyDown() {
                            localStart(localid);
                        }
                        @Override
                        public void onKeyUp() {
                            localEnd(localid);
                        }
                        @Override
                        public void onKeyAbort() {
                            localAbort(localid);
                        }
                        @Override
                        public Identifier getIcon() {
                            return Resources.getTexture("abilities/teleporter/flashing/" + strs[localid]);
                        }

                        @Override
                        public int createID() {
                            return localid;
                        }

                        public Skill getSkill() {
                            return instance;
                        }
                    });
                }
            }
        }

        @Environment(EnvType.CLIENT)
        void localStart(int keyid) {
            performingKey = keyid;

            startEffects();
        }

        @Environment(EnvType.CLIENT)
        void localEnd(int keyid) {
            if (keyid != performingKey) {
                return;
            }

            endEffects();

            sendToServer(MSG_PERFORM, performingKey);

            performingKey = -1;
        }

        @Environment(EnvType.CLIENT)
        void localAbort(int localid) {
            if (performingKey == localid) {
                performingKey = -1;
                endEffects();
            }
        }

        @Environment(EnvType.CLIENT)
        @Listener(channel=MSG_TICK, side=Side.CLIENT)
        void localTick() {
            if (isLocal()) {
                if (performingKey != -1 && !consume(true)) {
                    performingKey = -1;
                    endEffects();
                } else {
                    if (marking != null) {
                        Vec3 dest = getDest(performingKey);
                        marking.setPosition(dest.x, dest.y, dest.z);
                    }
                }

                if (cancellor != null && cancellor.isDead())
                    cancellor = null;
            }
        }

        @Listener(channel=MSG_TICK, side=Side.SERVER)
        void serverTick() {
            if(ctx.cpData.getOverload() < overloadKeep) ctx.cpData.setOverload(overloadKeep);
            if(ticks > max_time) terminate();
            ticks++;
        }

        @Listener(channel=MSG_PERFORM, side=Side.SERVER)
        void serverPerform(int keyid) {
            if (ctx.consume(0, consumption)) {
                Vec3 dest = getDest(keyid);
                if(player.isPassenger())
                    player.stopRiding();
                player.teleportTo(dest.x, dest.y, dest.z);
                player.resetFallDistance();

                ctx.addSkillExp(.002f);
                TPSkillHelper.incrTPCount(player);

                sendToClient(MSG_PERFORM);
            }
        }

        @Listener(channel=MSG_PERFORM, side=Side.CLIENT)
        @Environment(EnvType.CLIENT)
        void clientPerform() {
            ACSounds.playClient(player, "tp.tp_flashing", SoundSource.AMBIENT, 1.0f);
            if (isLocal()) {
                if (cancellor != null) {
                    cancellor.setDead();
                    cancellor = null;
                }
                cancellor = new GravityCancellor(player, 40);
                EventHub.BUS.register(cancellor);
            }
        }

        @Listener(channel=MSG_TERMINATED, side=Side.CLIENT)
        @Environment(EnvType.CLIENT)
        void localTerminate() {
            if (isLocal()) {
                clientRuntime().removeActiveHandler(activateHandler);
                clientRuntime().clearKeys(KEY_GROUP);
                endEffects();
            }
        }

        @Listener(channel=MSG_TERMINATED, side=Side.SERVER)
        void serverTerminated() {
            ctx.setCooldown(cooldown_time);
        }

        @Environment(EnvType.CLIENT)
        private void startEffects() {
            endEffects();

            marking = new EntityTPMarking(player);
            Vec3 dest = getDest(performingKey);
            marking.setPosition(dest.x, dest.y, dest.z);

            world().addFreshEntity(marking);
        }

        @Environment(EnvType.CLIENT)
        private void endEffects() {
            if (marking != null) {
                marking.discard();
                marking = null;
            }
        }

        private boolean consume(boolean simulate) {
            return simulate ? ctx.canConsumeCP(consumption) : ctx.consume(0, consumption);
        }

        private Vec3 getDest(int keyid) {
            Preconditions.checkState(keyid != -1);

            double dist = lerpf(12, 18, exp);

            Vec3 dir = VecUtils.copy(dirs[keyid]);
            dir = VecUtils.rotateAroundZ(dir, player.getXRot() * MathUtils.PI_F / 180)
                    .yRot((-90 - player.getYRot()) * MathUtils.PI_F / 180);


            Vec3 dst = VecUtils.add(player.getEyePosition(1F),VecUtils.multiply(dir, dist));
            RayTraceResult mop = Raytrace.perform(player.level(), player.position(),
                    dst, EntitySelectors.living().and(EntitySelectors.exclude(player)));

            double x, y, z;

            if (mop.typeOfHit != RayTraceResult.Type.MISS) {
                x = mop.hitVec.x;
                y = mop.hitVec.y;
                z = mop.hitVec.z;

                if (mop.typeOfHit == RayTraceResult.Type.BLOCK) {
                    switch (mop.sideHit) {
                        case DOWN:
                            y -= 1.0;
                            break;
                        case UP:
                            y += 1.8;
                            break;
                        case NORTH:
                            z -= .6;
                            y = mop.hitVec.y + 1.7;
                            break;
                        case SOUTH:
                            z += .6;
                            y = mop.hitVec.y + 1.7;
                            break;
                        case WEST:
                            x -= .6;
                            y = mop.hitVec.y + 1.7;
                            break;
                        case EAST:
                            x += .6;
                            y = mop.hitVec.y + 1.7;
                            break;
                    }
                    // check head
                    if (mop.sideHit.get3DDataValue() > 1) {
                        int hx = (int) x, hy = (int) (y + 1), hz = (int) z;
                        if (!player.level().isEmptyBlock(new BlockPos(hx, hy, hz))) {
                            y -= 1.25;
                        }
                    }
                } else {
                    y += mop.entityHit.getEyeHeight();
                }
            } else {
                x = dst.x;
                y = dst.y;
                z = dst.z;
            }

            return new Vec3(x, y, z);
        }

    }

}