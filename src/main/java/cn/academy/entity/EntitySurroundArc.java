package cn.academy.entity;
import cn.academy.ACEntities;
import net.minecraft.world.entity.EntityType;
import cn.lambdalib2.render.legacy.GL11;
import cn.academy.client.render.util.IPointFactory;
import cn.academy.client.render.util.CubePointFactory;
import cn.academy.client.render.util.ArcFactory;
import cn.academy.client.render.util.ArcFactory.Arc;
import cn.academy.client.render.util.SubArcHandler;
import cn.lambdalib2.util.Debug;
import cn.lambdalib2.util.entityx.EntityAdvanced;
import net.minecraft.client.Minecraft;
import cn.lambdalib2.render.legacy.LegacyRender;
import cn.lambdalib2.render.legacy.RenderManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import net.fabricmc.api.EnvType;



/**
 * Spawn a surround arc effect around the specific entity or block.
 * @author WeAthFolD
 */
@Environment(EnvType.CLIENT)
public class EntitySurroundArc extends EntityAdvanced {
    
    static final int TEMPLATES = 15;
    
    static {
        ArcFactory factory = new ArcFactory();
        factory.widthShrink = 0.9;
        factory.maxOffset = 0.8;
        factory.passes = 3;
        factory.width = 0.2;
        factory.branchFactor = 0.7;
        
        ArcType.THIN.templates = factory.generateList(10, 1.5, 2);
        
        factory.width = 0.3;
        ArcType.NORMAL.templates = factory.generateList(10, 3, 4);
        
        factory.passes = 3;
        factory.width = 0.35;
        factory.maxOffset =    1.2;
        factory.branchFactor = 0.45;
        ArcType.BOLD.templates = factory.generateList(10, 3.5, 4.5);
    }
    
    public enum ArcType {
        THIN(4), NORMAL(6), BOLD(5);
        
        public Arc[] templates;
        public int count;
        
        ArcType(int _count) {
            count = _count;
        }
    }

    private ArcType arcType = ArcType.BOLD;
    private final PosObject pos;
    
    public boolean draw = true;
    
    public int life = 100;

    SubArcHandler arcHandler;
    
    IPointFactory pointFactory;
    
    public EntitySurroundArc(Entity follow) {
        this(follow, 1.3);
    }
    
    public EntitySurroundArc(Entity follow, double sizeMultiplyer) {
        this(ACEntities.SURROUND_ARC, follow, sizeMultiplyer);
    }

    protected EntitySurroundArc(EntityType<?> type, Entity follow, double sizeMultiplyer) {
        super(type, follow.level());
        pos = new EntityPos(follow);
        setPosition(follow.getX(), follow.getY(), follow.getZ());
        pointFactory = new CubePointFactory(
            follow.getBbWidth() * sizeMultiplyer, 
            follow.getBbHeight() * sizeMultiplyer, 
            follow.getBbWidth() * sizeMultiplyer).setCentered(true);
    }
    
    public EntitySurroundArc(Level world, double x, double y, double z, double wl, double h) {
        super(ACEntities.SURROUND_ARC, world);
        setPos(x, y, z);
        pos = new ConstPos(x, y, z);
        pointFactory = new CubePointFactory(wl, h, wl).setCentered(true);
    }
    
    public void updatePos(double x, double y, double z) {
        pos.x = x;
        pos.y = y;
        pos.z = z;
    }
    
    public EntitySurroundArc setArcType(ArcType type) {
        arcType = type;
        return this;
    }
    
    public EntitySurroundArc setLife(int life) {
        this.life = life;
        return this;
    }
    
    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 1;
    }
    
    @Override
    public void onFirstUpdate() {
        // Create the arcs!
        arcHandler = new SubArcHandler(arcType.templates);
        arcHandler.frameRate = 0.6;
        arcHandler.switchRate = 0.7;
        
        doGenerate();
    }
    
    /**
     * Do the arc generation.
     */
    protected void doGenerate() {
        for(int i = 0; i < arcType.count; ++i) {
            double yaw = rand.nextDouble() * Math.PI * 2;
            double pitch = rand.nextDouble() * Math.PI;
            
            double y = Math.sin(pitch),
                zz = Math.sqrt(1 - y * y),
                x = zz * Math.sin(yaw),
                z = zz * Math.cos(yaw);
            
            arcHandler.generateAt(pointFactory.next());
        }
    }
    
    @Override
    public void onUpdate() {
        super.onUpdate();
        if(arcHandler.isEmpty())
            doGenerate();
        
        arcHandler.tick();
        
        pos.tick();
        setPosition(pos.x, pos.y, pos.z);
        setYRot(pos.yaw);
        setXRot(pos.pitch);
        
        if(tickCount == life)
            setDead();
    }

    @Override
    protected void readEntityFromNBT(CompoundTag tag) {
        setDead();
    }

    @Override
    protected void writeEntityToNBT(CompoundTag tag) {}
    
    private abstract class PosObject {
        double x, y, z;
        float yaw, pitch;
        
        void tick() {}
    }
    public static class Renderer extends LegacyRender<EntitySurroundArc> {

        public Renderer(RenderManager renderManager) {
            super(renderManager);
        }

        @Override
        public void doRender(EntitySurroundArc esa, double x,
                double y, double z, float a,
                float b) {
            if(esa.draw && esa.arcHandler != null) {
                GL11.glPushMatrix();
                
                GL11.glTranslated(x, y, z);
                
                GL11.glRotatef(-esa.getYRot(), 0, 1, 0);
                esa.arcHandler.drawAll();
                
                GL11.glPopMatrix();
            }
        }

        @Nullable
        @Override
        protected Identifier getEntityTexture(EntitySurroundArc entity) {
            return null;
        }

    }
    
    private class EntityPos extends PosObject {
        
        final Entity entity;
        final boolean isPlayer;
        
        public EntityPos(Entity e) {
            entity = e;
            isPlayer = e instanceof Player && e.equals(Minecraft.getInstance().player);
        }
        
        @Override
        void tick() {
            x = entity.getX();
            y = entity.getY();
            z = entity.getZ();
            yaw = entity instanceof LivingEntity ? ((LivingEntity)entity).yHeadRot : entity.getYRot();
            pitch = entity.getXRot();
        }
    }
    
    private class ConstPos extends PosObject {
        
        public ConstPos(double _x, double _y, double _z) {
            x = _x;
            y = _y;
            z = _z;
        }
    }

}