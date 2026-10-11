package cn.lambdalib2.particle;

import cn.lambdalib2.LLEntities;
import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.entityx.EntityAdvanced;
import cn.lambdalib2.util.entityx.handlers.Rigidbody;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A client-local particle entity.
 * @author WeAthFolD
 */
public final class Particle extends EntityAdvanced implements ISpriteEntity {

    public Identifier texture = null;
    public Color color = Colors.white();
    public float size = 1.0f;
    public boolean hasLight = false;
    public double gravity = 0.0;
    public boolean needRigidbody = true;
    /**
     * When set to true this particle is rotated with yaw and
     * pitch, else always faces the player.
     */
    public boolean customRotation = false;

    double creationTime;

    public int fadeInTime = 5;
    public int fadeTime;
    public int life = 10000000;
    float startAlpha;

    boolean updated;

    public Particle(EntityType<? extends Particle> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void fadeAfter(int life, int fadeTime) {
        this.life = life;
        this.fadeTime = fadeTime;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (tickCount > life) {
            int dt = tickCount - life;
            float alpha = fadeTime <= 0 ? -1 : 1 - (float) dt / fadeTime;
            if (alpha < 0) {
                discard();
                alpha = 0;
            }
            color.setAlpha(Colors.f2i(alpha * startAlpha));
        } else if (tickCount < fadeInTime) {
            color.setAlpha(Colors.f2i(startAlpha * ((float) tickCount / fadeInTime)));
        } else {
            color.setAlpha(Colors.f2i(startAlpha));
        }

        Vec3 m = getDeltaMovement();
        setDeltaMovement(m.x, m.y - gravity, m.z);
    }

    public void fromTemplate(ParticleTemplate template) {
        this.texture = template.texture;
        this.color = new Color(template.color);
        this.size = template.size;
        this.hasLight = template.hasLight;
        this.fadeTime = template.fadeTime;
        this.life = template.life;
        this.gravity = template.gravity;
        this.needRigidbody = template.needRigidbody;
        this.fadeInTime = template.fadeInTime;
        this.customRotation = template.customRotation;
        this.updated = false;
    }

    @Override
    protected void onFirstUpdate() {
        updated = true;
        if (needRigidbody)
            this.addMotionHandler(new Rigidbody());
        creationTime = GameTimer.getTime();
        startAlpha = Colors.i2f(color.getAlpha());
    }

    public boolean isUpdated() {
        return updated;
    }

    public double getParticleLife() {
        return GameTimer.getTime() - creationTime;
    }

    public double getMaxLife() {
        return life;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void updateSprite(Sprite s) {
        s.cullFace = false;
        s.height = s.width = size;
        s.texture = texture;
        s.color = color;
        s.hasLight = hasLight;
    }

    @Override
    public boolean needViewOptimize() {
        return false;
    }

}
