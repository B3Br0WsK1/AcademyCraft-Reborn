package cn.academy.ability.vanilla.vecmanip.skill;

import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.DelegateState;
import cn.academy.ability.context.IStateProvider;
import cn.academy.ability.context.RegClientContext;
import cn.academy.ability.vanilla.vecmanip.client.effect.PlasmaBodyEffect;
import cn.academy.ability.vanilla.vecmanip.client.effect.TornadoEffect;
import cn.academy.ability.vanilla.vecmanip.client.effect.TornadoRenderer;
import cn.academy.client.sound.ACSounds;
import cn.academy.client.sound.FollowEntitySound;
import cn.academy.entity.LocalEntity;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.EntitySelectors;
import cn.lambdalib2.util.RayTraceResult;
import cn.lambdalib2.util.Raytrace;
import cn.lambdalib2.util.Side;
import cn.lambdalib2.util.WorldUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import static cn.lambdalib2.render.legacy.GL11.*;
import static cn.lambdalib2.util.MathUtils.lerpf;
import static cn.lambdalib2.util.VecUtils.add;
import static cn.lambdalib2.util.VecUtils.multiply;
import static cn.lambdalib2.util.VecUtils.subtract;

/**
 * Plasma cannon. Ported from Scala.
 */
public class PlasmaCannon extends Skill {

    public static final PlasmaCannon INSTANCE = new PlasmaCannon();

    private PlasmaCannon() {
        super("plasma_cannon", 5);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        activateSingleKey(rt, keyID, PlasmaCannonContext::new);
    }

    public static final String MSG_PERFORM = "perform";
    public static final String MSG_STATECHG = "state_change";
    public static final String MSG_SYNCPOS = "sync_pos";

    public static final int STATE_CHARGING = 0;
    public static final int STATE_GO = 1;

    public static final double MOVING_SPEED = 1;

    public static class PlasmaCannonContext extends Context<PlasmaCannon> implements IStateProvider {

        public int localTicker = 0;
        public int syncTicker = 0;

        public int state = STATE_CHARGING;

        public Vec3 chargePosition = add(player.position(), new Vec3(0.0, 15.0, 0.0));
        public Vec3 destination = null;

        private final float chargeTime = lerpf(60, 30, ctx.getSkillExp());
        private final float overloadToKeep = lerpf(500, 400, ctx.getSkillExp());
        private float overloadKeep = 0f;

        public PlasmaCannonContext(Player p) {
            super(p, INSTANCE);
        }

        @Listener(channel=MSG_KEYUP, side=Side.CLIENT)
        public void l_keyUp() {
            if (localTicker >= chargeTime) {
                sendToServer(MSG_PERFORM);
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
            localTicker += 1;

            if (state == STATE_CHARGING && localTicker < chargeTime) {
                tryConsume();
            }

            if (state == STATE_CHARGING && localTicker == (int) chargeTime) {
                ACSounds.playClient(player, "vecmanip.plasma_cannon_t", SoundSource.AMBIENT, 0.5f);
            }
        }

        @Listener(channel=MSG_MADEALIVE, side=Side.SERVER)
        public void s_madeAlive() {
            ctx.consume(overloadToKeep, 0);
            overloadKeep = ctx.cpData.getOverload();
        }

        @Listener(channel=MSG_PERFORM, side=Side.SERVER)
        public void s_perform() {
            ctx.addSkillExp(0.008f);

            destination = Raytrace.getLookingPos(player, 100, EntitySelectors.living()).getLeft();
            state = STATE_GO;
            localTicker = 0;
            ctx.setCooldown((int) lerpf(1000, 600, ctx.getSkillExp()));
            sendToClient(MSG_STATECHG, destination);
        }

        @Listener(channel=MSG_STATECHG, side=Side.CLIENT)
        public void c_stateChange(Vec3 dest) {
            state = STATE_GO;
            destination = dest;
        }

        @Listener(channel=MSG_SYNCPOS, side=Side.CLIENT)
        public void c_syncPos(Vec3 pos) {
            chargePosition = pos;
        }

        @Listener(channel=MSG_TICK, side=Side.SERVER)
        public void s_tick() {
            if (ctx.cpData.getOverload() < overloadKeep) ctx.cpData.setOverload(overloadKeep);
            localTicker += 1;

            if (state == STATE_CHARGING) {
                if (localTicker < chargeTime) {
                    if (!tryConsume()) {
                        terminate();
                    }
                }
            } else if (state == STATE_GO) {
                Vec3 lastPos = chargePosition;

                tryMove();

                RayTraceResult trace = Raytrace.perform(world(), lastPos, chargePosition);
                if (trace != null && trace.typeOfHit != RayTraceResult.Type.MISS) {
                    explode();
                    return;
                }

                if (localTicker >= 240 || chargePosition.distanceTo(destination) < 1.5) {
                    explode();
                    return;
                }

                if (syncTicker == 0) {
                    syncTicker = 5;
                    sendToClient(MSG_SYNCPOS, chargePosition);
                } else {
                    syncTicker -= 1;
                }
            }
        }

        private void explode() {
            for (Entity entity : WorldUtils.getEntities(world(),
                destination.x, destination.y, destination.z, 10, EntitySelectors.everything())) {
                ctx.attack(entity, lerpf(80, 150, ctx.getSkillExp()));
                entity.setInvulnerableTime(-1);
            }

            if (world() instanceof ServerLevel sl) {
                Level.ExplosionInteraction interaction = ctx.canBreakBlock(world())
                    ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
                sl.explode(player, destination.x, destination.y, destination.z,
                    lerpf(12.0f, 15.0f, ctx.getSkillExp()), false, interaction);
            }

            terminate();
        }

        public boolean tryConsume() {
            float cp = lerpf(18, 25, ctx.getSkillExp());
            return ctx.consume(0, cp);
        }

        @Override
        public DelegateState getState() {
            if (state == STATE_CHARGING)
                return localTicker < chargeTime ? DelegateState.CHARGE : DelegateState.ACTIVE;
            else
                return DelegateState.ACTIVE;
        }

        void tryMove() {
            Vec3 rawDelta = subtract(destination, chargePosition);
            if (rawDelta.length() < 1) return;

            Vec3 delta = multiply(rawDelta.normalize(), MOVING_SPEED);
            chargePosition = add(chargePosition, delta);
        }

    }

    @Environment(EnvType.CLIENT)
    static class Tornado extends LocalEntity {

        final TornadoEffect theTornado = new TornadoEffect(12, 8, 1, 0.3);

        final PlasmaCannonContext ctx;

        boolean dead = false;
        int deadTick = 0;

        Tornado(PlasmaCannonContext ctx) {
            super(ctx.player.level());
            this.ctx = ctx;

            Vec3 p0 = ctx.chargePosition;
            Vec3 p1 = new Vec3(p0.x, p0.y - 20.0, p0.z);
            RayTraceResult result = Raytrace.perform(ctx.player.level(), p0, p1, EntitySelectors.nothing());
            Vec3 initPos = (result != null && result.typeOfHit != RayTraceResult.Type.MISS) ? result.hitVec : p1;
            setPos(initPos.x, initPos.y, initPos.z);
        }

        @Override
        public void onUpdate() {
            if (ctx.state == STATE_GO || ctx.getStatus() == Context.Status.TERMINATED) {
                dead = true;
            }

            if (dead) {
                deadTick += 1;
                if (deadTick == 30) {
                    setDead();
                }
            }

            theTornado.alpha = alpha() * 0.5f;
        }

        float alpha() {
            if (!dead) {
                return tickCount < 20.0f ? tickCount / 20.0f : 1.0f;
            } else {
                return 1 - deadTick / 20.0f;
            }
        }

        @Override
        public void renderEffect(double x, double y, double z, float partialTicks) {
            glPushMatrix();
            glTranslated(x, y, z);

            glDisable(GL_ALPHA_TEST);
            TornadoRenderer.doRender(theTornado);

            glPopMatrix();
        }

    }

    @Environment(EnvType.CLIENT)
    @RegClientContext(PlasmaCannonContext.class)
    public static class PlasmaCannonContextC extends ClientContext {

        private FollowEntitySound sound;

        private PlasmaBodyEffect effect;

        public PlasmaCannonContextC(Context par) {
            super(par);
        }

        private PlasmaCannonContext self() {
            return (PlasmaCannonContext) parent;
        }

        @Listener(channel=MSG_MADEALIVE, side=Side.CLIENT)
        private void c_begin() {
            effect = new PlasmaBodyEffect(world(), self());
            effect.setPos(self().chargePosition.x, self().chargePosition.y, self().chargePosition.z);

            world().addFreshEntity(new Tornado(self()));
            world().addFreshEntity(effect);

            sound = new FollowEntitySound(player, "vecmanip.plasma_cannon", SoundSource.AMBIENT);
            ACSounds.playClient(sound);
        }

        @Listener(channel=MSG_TERMINATED, side=Side.CLIENT)
        private void c_terminate() {
            if (sound != null) sound.stop();
        }

        @Listener(channel=MSG_TICK, side=Side.CLIENT)
        private void c_tick() {
            if (self().state == STATE_GO) {
                self().tryMove();
            }
            if (effect != null)
                effect.setPos(self().chargePosition.x, self().chargePosition.y, self().chargePosition.z);
        }

    }

}
