package cn.academy.ability.vanilla.teleporter.skill;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.ability.vanilla.teleporter.util.TPSkillHelper;
import cn.academy.client.render.util.ACRenderingHelper;
import cn.academy.client.sound.ACSounds;
import cn.academy.entity.EntityBloodSplash;
import cn.academy.entity.EntityMarker;
import cn.lambdalib2.s11n.SerializeIncluded;
import cn.lambdalib2.s11n.SerializeNullable;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.NetworkS11nType;
import cn.lambdalib2.util.EntitySelectors;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.Raytrace;
import cn.lambdalib2.util.VecUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.sounds.SoundSource;
import cn.lambdalib2.util.RayTraceResult;
import net.minecraft.world.phys.Vec3;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import cn.lambdalib2.util.Color;
import static cn.lambdalib2.util.MathUtils.lerpf;
import net.fabricmc.api.EnvType;



/**
 * @author WeAthFolD, KSkun
 */
public class FleshRipping extends Skill
{
    public static final FleshRipping instance = new FleshRipping();
    public FleshRipping()
    {
        super("flesh_ripping", 3);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void activate(ClientRuntime rt , int keyID){
        activateSingleKey2(rt, keyID, FRContext::new);
    }

    static final String MSG_ABORT = "abort";
    static final String MSG_END = "end";
    static final String MSG_EFFECT_END = "effect_end";

    public static class FRContext extends Context{

        private float exp = ctx.getSkillExp();

        private AttackTarget target = null;

        public FRContext(Player p)
        {
            super(p, instance);
        }

        @Listener(channel=MSG_TICK, side=Side.SERVER)
        private void s_tick(){
            if(!ctx.canConsumeCP(getConsumption()))
                terminate();
            target = getAttackTarget();
        }

        @Listener(channel=MSG_END, side=Side.SERVER)
        private void s_end(){
            sendToClient(MSG_EFFECT_END, target);
            if(target.target == null)
                sendToSelf(MSG_ABORT);
            else {
                ctx.consumeWithForce(getOverload(), getConsumption());
                TPSkillHelper.attackIgnoreArmor(ctx, target.target, getDamage());
                if(RandUtils.ranged(0, 1) < getDisgustProb())
                    player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 100));
                ctx.setCooldown((int)lerpf(90, 40, exp));
                ctx.addSkillExp(.005f);
            }
            terminate();
        }

        @Listener(channel=MSG_KEYUP, side=Side.CLIENT)
        private void l_onKeyUp() {
            sendToServer(MSG_END);
        }

        @Listener(channel=MSG_KEYABORT, side=Side.CLIENT)
        private void l_onKeyAbort(){
            sendToSelf(MSG_EFFECT_END, new AttackTarget());
            terminate();
        }

        @Listener(channel=MSG_ABORT, side=Side.SERVER)
        private void s_abort() {
            target = null;
        }

        private float getDamage(){
            return lerpf(5, 12, exp);
        }

        @Override
        public double getRange(){
            return lerpf(6, 14, exp);
        }

        private float getDisgustProb(){
            return .05f;
        }

        private float getConsumption() {
            return lerpf(130, 270, exp);
        }

        private float getOverload() {
            return lerpf(60, 50, exp);
        }

        public AttackTarget getAttackTarget(){
            double range = getRange();
            RayTraceResult trace  = Raytrace.traceLiving(player, range, EntitySelectors.living());
            Entity target  = null;
            Vec3 dest  = null;
            if (trace != null) {
                target = trace.entityHit;
                dest = trace.hitVec;
            }
            else dest = VecUtils.add(player.position(), VecUtils.multiply(player.getLookAngle(), range));
            return new AttackTarget(dest, target, player);
        }
    }

    @Environment(EnvType.CLIENT)
    @RegClientContext(FRContext.class)
    static class FRContextC extends ClientContext
    {
        FRContext par;
        public FRContextC(FRContext par)
        {
            super(par);
            this.par = par;
        }


        private EntityMarker marker = null;

        private Color DISABLED_COLOR  = new Color(74, 74, 74, 160);
        private Color THREATENING_COLOR = new Color(185, 25, 25, 180);

        @Listener(channel=MSG_TERMINATED, side=Side.CLIENT)
        private void l_terminated(){
            if(marker != null)
                marker.discard();
        }

        @Listener(channel=MSG_MADEALIVE, side=Side.CLIENT)
        private void l_startEffect(){
        if(isLocal()) {
            marker = new EntityMarker(player.level());
            marker.setPosition(player.getX(), player.getY(), player.getZ());
            marker.color = DISABLED_COLOR;
            player.level().addFreshEntity(marker);
        }
      }

        @Listener(channel=MSG_TICK, side=Side.CLIENT)
        private void l_updateEffect(){
        if(isLocal()) {
            AttackTarget at  = par.getAttackTarget();
            marker.setPosition(at.dest.x, at.dest.y, at.dest.z);
            if (at.target == null) {
                marker.color = DISABLED_COLOR;
                marker.width = 1.0f;
                marker.height = 1.0f;
            }
            else {
                marker.color = THREATENING_COLOR;
                marker.width = at.target.getBbWidth() * 1.2f;
                marker.height = at.target.getBbHeight() * 1.2f;
            }
        }
      }

        @Listener(channel=MSG_EFFECT_END, side=Side.CLIENT)
        private void c_endEffect(AttackTarget target){
            if(isLocal())
                marker.discard();

            if(target != null && target.target != null) {
                ACSounds.playClient(player, "tp.guts", SoundSource.AMBIENT, 0.6f);
                Entity e  = target.target;
                for(int i= 0,max = RandUtils.rangei(4, 6);i<max;i++) {
                    double y = e.getY() + RandUtils.ranged(0, 1) * e.getBbHeight();
                    if(e instanceof Player )
                        y += ACRenderingHelper.getHeightFix((Player) e);
                    double theta = RandUtils.ranged(0, Math.PI * 2);
                    double r = 0.5 * RandUtils.ranged(0.8 * e.getBbWidth(), e.getBbWidth());
                    EntityBloodSplash splash = new EntityBloodSplash(player.level());
                    splash.setPosition(e.getX() + r * Math.sin(theta), y, e.getZ() + r * Math.cos(theta));
                    player.level().addFreshEntity(splash);
                }
            }
        }

    }


    @NetworkS11nType
    static class AttackTarget{
        @SerializeIncluded
        Vec3 dest = null;
        @SerializeIncluded
        @SerializeNullable
        Entity target = null;
        @SerializeIncluded
        Player player = null;

        public AttackTarget(){ }

        public AttackTarget(Vec3 _dest , Entity _target, Player _player) {
            dest = _dest;
            target = _target;
            player = _player;
        }

        public AttackTarget(CompoundTag tag, Player _player) {
            this(new Vec3(tag.getFloatOr("x", 0f), tag.getFloatOr("y", 0f), tag.getFloatOr("z", 0f)),
                _player.level().getEntity(tag.getIntOr("i", 0)), _player);
        }

        private CompoundTag toNBT(){
            CompoundTag ret = new CompoundTag();
            ret.putFloat("x", (float) dest.x);
            ret.putFloat("y", (float) dest.y);
            ret.putFloat("z", (float) dest.z);
            ret.putInt("i", (target == null)? 0 : target.getId());
            return ret;
        }

    }
}
