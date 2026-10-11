package cn.academy.entity;
import cn.lambdalib2.render.legacy.GL11;
import cn.lambdalib2.render.legacy.GL20;
import cn.academy.client.render.util.ACRenderingHelper;
import cn.academy.Resources;
import cn.lambdalib2.render.legacy.ShaderSimple;
import cn.lambdalib2.template.client.render.RenderIcon;
import cn.lambdalib2.util.*;
import cn.lambdalib2.util.entityx.EntityAdvanced;
import cn.lambdalib2.util.entityx.EntityCallback;
import net.minecraft.client.Minecraft;
import cn.lambdalib2.render.legacy.RenderManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import cn.academy.ACEntities;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;


/**
 * @author WeAthFolD
 */
public class EntityMdBall extends EntityAdvanced
{
    private static final EntityDataAccessor<Integer> SPAWNER_ID = SynchedEntityData.defineId(EntityMdBall.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SUB_X = SynchedEntityData.defineId(EntityMdBall.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SUB_Y = SynchedEntityData.defineId(EntityMdBall.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SUB_Z = SynchedEntityData.defineId(EntityMdBall.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(EntityMdBall.class, EntityDataSerializers.INT);

    static final int MAX_TETXURES = 5;
    
    static final float RANGE_FROM = 0.8f, RANGE_TO = 1.3f;
    
    //Synced states
    Player spawner;
    float subX = 0, subY = 0, subZ = 0;
    
    //Ctor init data
    int life = 50;
    
    //Client-side data
    int texID;
    
    double spawnTime;
    double lastTime;
    double burstTime = 0.4;
    float alphaWiggle = 0.8f;
    double accel;
    
    double offsetX, offsetY, offsetZ;
    
    public EntityMdBall(Player player) {
        this(player, 2333333, null);
    }
    
    public EntityMdBall(Player player, int life) {
        this(player, life, null);
    }
    
    public EntityMdBall(Player player, int life, final EntityCallback<EntityMdBall> callback) {
        this(ACEntities.MD_BALL, player.level());
        this.spawner = player;
        
        // Calc the sub-offset
        float theta = -player.getYRot() / 180 * MathUtils.PI_F +
            RandUtils.rangef(-MathUtils.PI_F * 0.45f, MathUtils.PI_F * 0.45f);
        
        float range = RandUtils.rangef(RANGE_FROM, RANGE_TO);
        subX = Mth.sin(theta) * range;
        subZ = Mth.cos(theta) * range;
        
        subY = RandUtils.rangef(-1.2f, 0.2f);
        
        // Pos init
        updatePosition();
        
        this.life = life;

        this.executeAfter((EntityCallback<EntityMdBall>) e -> e.setDead(), life);
        if(callback != null)
            this.executeAfter(callback, life - 2);
    }
    
    public EntityMdBall(EntityType<? extends EntityMdBall> type, Level world) {
        super(type, world);
        spawnTime = GameTimer.getTime();
    }
    
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SPAWNER_ID, 0);
        builder.define(SUB_X, 0F);
        builder.define(SUB_Y, 0F);
        builder.define(SUB_Z, 0F);
        builder.define(LIFE, 0);
    }
    
    @Override
    public void onFirstUpdate() {
        if(!world.isClientSide()) {
            this.entityData.set(SPAWNER_ID, spawner.getId());
            this.entityData.set(SUB_X, subX);
            this.entityData.set(SUB_Y, subY);
            this.entityData.set(SUB_Z, subZ);
            this.entityData.set(LIFE, life);
        }
    }
    
    @Override
    public void onUpdate() {
        super.onUpdate();
        
        if(world.isClientSide()) {
            
            if(getSpawner() == null) {
                int eid = this.entityData.get(SPAWNER_ID);
                Entity e = world.getEntity(eid);
                if(e instanceof Player) {
                    spawner = (Player) e;
                }
                
            } else {
                if(subX == 0 && subY == 0 && subZ == 0) {
                    subX = this.entityData.get(SUB_X);
                    subY = this.entityData.get(SUB_Y);
                    subZ = this.entityData.get(SUB_Z);
                    life = this.entityData.get(LIFE);
                } else {
                    updatePosition();
                }
            }
            
        } else {
            
            updatePosition();
            
        }
    }
    
    public Player getSpawner() {
        return spawner;
    }
    
    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 1;
    }

    @Override
    protected void readEntityFromNBT(CompoundTag tag) {
        setDead();
    }

    @Override
    protected void writeEntityToNBT(CompoundTag tag) {}
    
    @Environment(EnvType.CLIENT)
    private boolean updateRenderTick() {
        
        if(spawner == null || (subX == 0 && subY == 0 && subZ == 0)) 
            return false;
        
        final double maxAccel = 4;
        double time = GameTimer.getTime();
        double life = time - spawnTime;
        
        //Alpha wiggling
        if(lastTime != 0) {
            double dt = time - lastTime;
            if(rand.nextInt(8) < 3) {
                accel = RandUtils.ranged(-maxAccel, maxAccel);
                //System.out.println("AccelChange=>" + accel);
            }
            
            //System.out.println("AV=>" + alphaVel);
            alphaWiggle += accel * dt;
            if(alphaWiggle > 1) alphaWiggle = 1;
            if(alphaWiggle < 0) alphaWiggle = 0;
        }
        lastTime = time;
        
        //Texture wiggling
        if(rand.nextInt(8) < 2) {
            texID = rand.nextInt(MAX_TETXURES);
        }
        
        //Surrounding
        float phase = (float) (life / 0.3f);
        offsetX = 0.03 * Mth.sin(phase);
        offsetZ = 0.03 * Mth.cos(phase);
        offsetY = 0.04 * Mth.cos((float) (phase * 1.4 + Math.PI / 3.5));
        
        updatePosition();
        
        return true;
    }
    
    private float getAlpha() {
        float lifeS = life * 0.05f;
        double time = GameTimer.getTime();
        float dt = (float) (time - spawnTime);
        
        final float blendTime = 0.15f;
        if(dt > lifeS - blendTime)
            return Math.max(0, MathUtils.lerpf(1, 0, (dt - (lifeS - blendTime)) / blendTime));
        if(dt > lifeS - burstTime)
            return MathUtils.lerpf(0.6f, 1.0f, (float) ((dt - (lifeS - burstTime)) / (burstTime - blendTime)) );
        if(dt < 0.3f)
            return MathUtils.lerpf(0, 0.6f, dt / 0.3f);
        return 0.6f;
    }
    
    private float getSize() {
        int lifeMS = life * 50;
        double time = GameTimer.getTime();
        double dt = time - spawnTime;
        
        if(dt > lifeMS - 100)
            return Math.max(0, MathUtils.lerpf(1.5f, 0, (float) (dt - (lifeMS - 100)) / 100));
        if(dt > lifeMS - 300)
            return MathUtils.lerpf(1, 1.5f, (float) (dt - (lifeMS - 300)) / 200);
        return 1;
    }
    
    /**
     * Height above the entity position where the ball is drawn (and where its rays start), as in the original.
     */
    public static final double VISUAL_HEIGHT = 1.6;

    /**
     * @return The point where the ball is visually located.
     */
    public Vec3 getVisualPos() {
        return new Vec3(getX(), getY() + VISUAL_HEIGHT, getZ());
    }

    private void updatePosition() {
        setPosition(
            spawner.getX() + subX,
            spawner.getY() + subY,
            spawner.getZ() + subZ
        );
    }
    @Environment(EnvType.CLIENT)
    public static class R extends RenderIcon<EntityMdBall> {
        
        Identifier[] textures;
        Identifier glowTexture;

        public R(RenderManager manager) {
            super(manager, null);
            textures = Resources.getEffectSeq("mdball", MAX_TETXURES);
            glowTexture = Resources.getTexture("effects/mdball/glow");
            //this.minTolerateAlpha = 0.05f;
            this.shadowOpaque = 0;
        }
        
        @Override
        public void doRender(EntityMdBall ent, double x, double y,
                double z, float par8, float par9) {
            if(RenderUtils.isInShadowPass()) {
                return;
            }
            
            if(!ent.updateRenderTick())
                return;
            
            // Use the interpolated spawner position to prevent glitches
            if (ent.spawner != null) {
                Vec3 cam = cn.lambdalib2.render.legacy.LegacyGL.cameraPos();
                Vec3 sp = ent.spawner.getPosition(par9);
                x = sp.x + ent.subX - cam.x;
                // The original rendered the ball 1.6 above its position (eye level); damage traces also start
                //  at ball position + eye height.
                y = sp.y + ent.subY + VISUAL_HEIGHT - cam.y;
                z = sp.z + ent.subZ - cam.z;
            }

            GL11.glDepthMask(false);

            GL11.glPushMatrix();
            {
                ShaderSimple.instance().useProgram();
                GL11.glTranslated(ent.offsetX, ent.offsetY, ent.offsetZ);
                
                float alpha = ent.getAlpha();
                float size = ent.getSize();
                
                //Glow texture
                this.color.setAlpha(Colors.f2i(alpha * (0.3f + ent.alphaWiggle * 0.7f)));
                this.icon = glowTexture;
                super.doRender(ent, x, y, z, par8, par9);
                
                //Core
                this.color.setAlpha(Colors.f2i(alpha * (0.8f + 0.2f * ent.alphaWiggle)));
                this.icon = textures[ent.texID];
                super.doRender(ent, x, y, z, par8, par9);
                GL20.glUseProgram(0);
            }
            GL11.glPopMatrix();
        }
        
    }

}