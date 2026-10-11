package cn.lambdalib2.particle;

import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.Colors;
import net.minecraft.resources.Identifier;

/**
 * Template properties copied to particles created by a {@link ParticleFactory}. (In 1.12 the template was itself a
 *  Particle entity; entities can no longer be created without a level.)
 */
public class ParticleTemplate {

    public Identifier texture = null;
    public Color color = Colors.white();
    public float size = 1.0f;
    public boolean hasLight = false;
    public double gravity = 0.0;
    public boolean needRigidbody = true;
    public boolean customRotation = false;
    public int fadeInTime = 5;
    public int fadeTime;
    public int life = 10000000;

    public void fadeAfter(int life, int fadeTime) {
        this.life = life;
        this.fadeTime = fadeTime;
    }

}
