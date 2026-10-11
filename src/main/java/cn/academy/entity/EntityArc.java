package cn.academy.entity;
import cn.academy.ACEntities;
import cn.lambdalib2.render.legacy.GL11;
import cn.academy.client.render.util.ArcFactory;
import cn.academy.client.render.util.ArcFactory.Arc;
import cn.lambdalib2.util.ViewOptimize;
import cn.lambdalib2.util.ViewOptimize.IAssociatePlayer;
import cn.lambdalib2.util.EntityLook;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.ViewOptimize;
import cn.lambdalib2.util.entityx.EntityAdvanced;
import cn.lambdalib2.render.legacy.LegacyRender;
import cn.lambdalib2.render.legacy.RenderManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;

/*
import cn.lambdalib2.util.entityx.EntityAdvanced;*/

/**
 * @author WeAthFolD
 *
 */
@Environment(EnvType.CLIENT)
public class EntityArc extends EntityAdvanced implements ViewOptimize.IAssociatePlayer
{
    
    static final int GEN = 20;
    
    // Default patterns
    static Arc[] defaultPatterns = new Arc[GEN];
    static {
        ArcFactory fac = new ArcFactory();
        for(int i = 0; i < GEN; ++i) {
            defaultPatterns[i] = fac.generate(20);
        }
    }

    final Arc[] patterns;
    
    int [] iid;
    int n = 1;//RandUtils.rangei(1, 2);
    boolean show = true;
    
    /**
     * LegacyRender properties
     */
    public double 
        showWiggle = .2,
        hideWiggle = .2,
        texWiggle = .5;
    
    public double length = 20.0;
    public boolean lengthFixed = true;
    
    public boolean viewOptimize = true;
    
    final Player player;

    public EntityArc(Player _player, Arc[]_patterns) {
        super(ACEntities.ARC, _player.level());
        this.player = _player;
        this.setPosition(player.getX(), player.getY() + _player.getEyeHeight(), player.getZ());
        new EntityLook(_player).applyToEntity(this);
        iid = new int[n];
        
        this.patterns = _patterns;
    }
    
    public EntityArc(Player _player) {
        this(_player, defaultPatterns);
    }
    
    @Override
    public void onUpdate() {
        super.onUpdate();
        for(int i = 0; i < iid.length; ++i) {
            if(rand.nextDouble() < texWiggle)
                iid[i] = rand.nextInt(patterns.length);
        }
        if(show && rand.nextDouble() < showWiggle) {
            show = !show;
        }
        else if(!show && rand.nextDouble() < hideWiggle) {
            show = !show;
        }
    }
    
    public void setFromTo(double x0, double y0, double z0, double x1, double y1, double z1) {
        setPosition(x0, y0, z0);
        
        double dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        double dxzsq = dx * dx + dz * dz;
        setYRot((float) (-Math.atan2(dx, dz) * 180 / Math.PI));
        setXRot((float) (-Math.atan2(dy, Math.sqrt(dxzsq)) * 180 / Math.PI));
        
        length = MathUtils.distance(x0, y0, z0, x1, y1, z1);
    }

    @Override
    protected void readEntityFromNBT(CompoundTag nbt) {
    }

    @Override
    protected void writeEntityToNBT(CompoundTag p_70014_1_) {
    }
    
    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 1;
    }

    @Environment(EnvType.CLIENT)
    public static class Renderer extends LegacyRender<EntityArc> {

        public Renderer(RenderManager renderManager) {
            super(renderManager);
        }

        @Override
        public void doRender(EntityArc arc, double x, double y, double z, float f, float g) {
            if(!arc.show)
                return;
            
            GL11.glPushMatrix();
            
            GL11.glTranslated(x, y, z);
            GL11.glRotatef(arc.getYRot() + 90, 0, -1, 0);
            GL11.glRotatef(arc.getXRot(), 0, 0, -1);

            if(arc.viewOptimize) {
                ViewOptimize.fix(arc);
            }
            
            if(arc.lengthFixed) {
                for(int i = 0; i < arc.n; ++i)
                    arc.patterns[arc.iid[i]].draw();
            } else {
                for(int i = 0; i < arc.n; ++i) {
                    arc.patterns[arc.iid[i]].draw(arc.length);
                }
            }
            
            GL11.glPopMatrix();
        }

        @Override
        protected Identifier getEntityTexture(EntityArc entity) {
            return null;
        }
        
    }

    @Override
    public Player getPlayer() {
        return player;
    }

}