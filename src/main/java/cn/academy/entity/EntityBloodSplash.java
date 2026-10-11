package cn.academy.entity;
import cn.academy.ACEntities;
import static cn.lambdalib2.render.legacy.GL11.*;
import cn.academy.Resources;
import cn.lambdalib2.template.client.render.RenderIcon;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.entityx.EntityAdvanced;
import cn.lambdalib2.render.legacy.RenderManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;



/**
 * @author WeAthFolD
 */
@Environment(EnvType.CLIENT)
public class EntityBloodSplash extends EntityAdvanced
{

    static Identifier[] SPLASH = Resources.getEffectSeq("blood_splash", 10);

    int frame;

    public EntityBloodSplash(Level world) {
        super(ACEntities.BLOOD_SPLASH, world);
    }

    private float size = 1;

    public void setSize(float size) {
        this.size = size;
    }

    public float getSize() {
        return this.size;
    }

    @Override
    public void onUpdate() {
        if (++frame == SPLASH.length) {
            setDead();
        }
        super.onUpdate();
    }

    @Override
    protected void readEntityFromNBT(CompoundTag tag) {
    }

    @Override
    protected void writeEntityToNBT(CompoundTag tag) {
    }

    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 1;
    }
    public static class SplashRenderer extends RenderIcon<EntityBloodSplash> {

        public SplashRenderer(RenderManager manager) {
            super(manager, null);
            this.color.set(213, 29, 29, 200);
        }

        @Override
        public void doRender(EntityBloodSplash entity, double x, double y, double z, float a, float b) {
            EntityBloodSplash splash = (EntityBloodSplash) entity;
            icon = (SPLASH[MathUtils.clampi(0, SPLASH.length - 1, splash.frame)]);
            this.size = splash.getSize();

            glDepthMask(false);
            super.doRender(entity, x, y, z, a, b);
            glDepthMask(true);
        }

    }

}