package cn.academy.entity;
import cn.lambdalib2.particle.ParticleTemplate;
import cn.lambdalib2.render.legacy.GL11;
import cn.academy.Resources;
import cn.academy.client.sound.ACSounds;
import cn.lambdalib2.particle.Particle;
import cn.lambdalib2.particle.ParticleFactory;
import cn.lambdalib2.particle.decorators.ParticleDecorator;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.render.obj.ObjLegacyRender;
import cn.lambdalib2.util.*;
import cn.lambdalib2.util.entityx.EntityAdvanced;
import cn.lambdalib2.util.entityx.EntityCallback;
import cn.lambdalib2.util.entityx.MotionHandler;
import cn.lambdalib2.util.entityx.event.CollideEvent;
import cn.lambdalib2.util.entityx.handlers.Rigidbody;
import cn.lambdalib2.render.legacy.LegacyRender;
import cn.lambdalib2.render.legacy.RenderManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import cn.academy.ACEntities;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import cn.lambdalib2.util.RayTraceResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;



/**
 * @author WeathFolD
 */
public class EntitySilbarn extends EntityAdvanced
{
    private static final EntityDataAccessor<Byte> HIT_SYNC = SynchedEntityData.defineId(EntitySilbarn.class, EntityDataSerializers.BYTE);


    @Environment(EnvType.CLIENT)
    static ParticleFactory particles;

    @Environment(EnvType.CLIENT)
    @StateEventCallback
    public static void init(StateEvents.Init event) {
        ParticleTemplate p = new ParticleTemplate();
        p.texture = Resources.getTexture("entities/silbarn_frag");
        p.size = 0.1f;
        p.gravity = 0.03f;
        p.customRotation = true;
        
        particles = new ParticleFactory(p);
        particles.addDecorator(new ParticleDecorator() {
            
            double vx, vy;
            final double fac = 25;
            {
                double phi = RandUtils.nextDouble() * Math.PI * 2;
                vx = Math.sin(phi);
                vy = Math.cos(phi);
            }

            @Environment(EnvType.CLIENT)
            @Override
            public void decorate(Particle particle) {
                particle.addMotionHandler(new MotionHandler() {
                    @Override
                    public String getID() { return "Rotator"; }
                    @Override
                    public void onStart() {
                        particle.setYRot(RandUtils.nextFloat() * 360);
                        particle.setXRot(RandUtils.rangef(-90, 90));
                    }

                    @Override
                    public void onUpdate() {
                        particle.setYRot((float) (particle.getYRot() + (vx * fac)));
                        particle.setXRot((float) (particle.getXRot() + (vy * fac)));
                    }
                });
            }
            
        });
    }
    
    boolean hit;
    
    double createTime;
    
    Vec3 axis = new Vec3(rand.nextInt(), rand.nextInt(), rand.nextInt());
    
    {
        final Rigidbody rigidbody = new Rigidbody();
        rigidbody.linearDrag = 0.8;
        rigidbody.entitySel = EntitySelectors.nothing();
        
        this.addMotionHandler(rigidbody);
        //this.addDaemonHandler(new GravityApply(this, 0.05));
        executeAfter((EntityCallback<EntitySilbarn>) ent -> rigidbody.gravity = 0.12, 50);
    }

    public EntitySilbarn(Player player) {
        super(ACEntities.SILBARN, player.level());
        this.regEventHandler(new CollideEvent.CollideHandler() {
            
            @Override
            public void onEvent(CollideEvent event) {
                if(!hit) {
                    hit = true;
                    if(event.result.entityHit instanceof EntitySilbarn)
                        playSound(Resources.sound("entity.silbarn_heavy"), 0.5f, 1.0f);
                    else
                        playSound(Resources.sound("entity.silbarn_light"), 0.5f, 1.0f);
                    executeAfter(ent -> setDead(), 10);
                }
            }
            
        });

        setPosition(player.getX(), player.getEyeY(), player.getZ());

        Vec3 look = player.getLookAngle();
        setDeltaMovement(look.x, getDeltaMovement().y, getDeltaMovement().z);
        setDeltaMovement(getDeltaMovement().x, look.y, getDeltaMovement().z);
        setDeltaMovement(getDeltaMovement().x, getDeltaMovement().y, look.z);
        
        this.setYRot(player.yHeadRot);
    }

    public EntitySilbarn(EntityType<? extends EntitySilbarn> type, Level world) {
        super(type, world);
        if(world.isClientSide())
        {
            this.createTime = GameTimer.getTime();

            this.regEventHandler(new CollideEvent.CollideHandler() {
                @Override
                public void onEvent(CollideEvent event) {
                    if(!hit) {
                        RayTraceResult res = event.result;
                        Direction dir = res.sideHit;
                        final double mul = 0.1;
                        double tx = res.hitVec.x + dir.getStepX() * mul,
                                ty = res.hitVec.y + dir.getStepY() * mul,
                                tz = res.hitVec.z + dir.getStepZ() * mul;
                        spawnEffects(tx, ty, tz);
                        setDead();
                    }
                    hit = true;
                }
            });
        }

        
    }
    
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(HIT_SYNC, (byte) 0);
    }
    
    public boolean isHit() {
        return hit;
    }
    
    @Override
    public void onUpdate() {
        super.onUpdate();
        sync();
    }

    private void sync() {
        if(world.isClientSide()) {
            boolean b = entityData.get(HIT_SYNC) != 0;
            if(!hit && b) {
                spawnEffects(getX(), getY(), getZ());
            }
            hit = b;
        } else {
            entityData.set(HIT_SYNC, (byte) (hit ? 1 : 0));
        }
    }
    
    @Override
    public boolean isPickable() {
        return !isRemoved();
    }
    
    @Environment(EnvType.CLIENT)
    private void spawnEffects(double tx, double ty, double tz) {
        int n = RandUtils.rangei(18, 27);
        for(int i = 0; i < n; ++i) {
            double vel = RandUtils.ranged(0.08, 0.18),
                vsq = vel * vel,
                vx = rand.nextDouble() * vel,
                vxsq = vx * vx,
                vy = rand.nextDouble() * Math.sqrt(vsq - vxsq),
                vz = Math.sqrt(vsq - vxsq - vy * vy);
            vx *= rand.nextBoolean() ? 1 : -1;
            vy *= rand.nextBoolean() ? 1 : -1;
            vz *= rand.nextBoolean() ? 1 : -1;
            vy += 0.2;
            
            particles.setPosition(getX(), getY(), getZ());
            particles.setVelocity(vx, vy, vz);
            world.addFreshEntity(particles.next(world));
        }
        //TileMatrix
    }
    
    @Environment(EnvType.CLIENT)
    public static class RenderSibarn extends LegacyRender<EntitySilbarn> {
        
        private final ObjLegacyRender model = Resources.getModel("silbarn");
        private final Identifier tex = Resources.getTexture("models/silbarn");

        public RenderSibarn(RenderManager renderManager) {
            super(renderManager);
        }

        @Override
        public void doRender(EntitySilbarn var1, double x, double y,
                double z, float var8, float var9) {
            EntitySilbarn sibarn = (EntitySilbarn) var1;
            if(sibarn.hit)
                return;
            GL11.glPushMatrix();
            GL11.glTranslated(x, y, z);
            RenderUtils.loadTexture(tex);
            double scale = .05;
            GL11.glScaled(scale, scale, scale);
            GL11.glRotated(0.03 * (long)((GameTimer.getTime() - sibarn.createTime) * 1000),
                    sibarn.axis.x, sibarn.axis.y, sibarn.axis.z);
            GL11.glRotated(-var1.getYRot(), 0, 1, 0);
            GL11.glRotated(90, 1, 0, 0);
            model.renderAll();
            GL11.glPopMatrix();
        }

        @Override
        protected Identifier getEntityTexture(EntitySilbarn entity) {
            return null;
        }
    }

    @Override
    protected void readEntityFromNBT(CompoundTag tag) {
        setDead();
    }

    @Override
    protected void writeEntityToNBT(CompoundTag tag) {}

}