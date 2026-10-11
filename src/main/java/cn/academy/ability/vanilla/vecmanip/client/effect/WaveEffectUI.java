package cn.academy.ability.vanilla.vecmanip.client.effect;

import cn.academy.Resources;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.HudUtils;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.RenderUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;

import java.util.Iterator;
import java.util.LinkedList;

import static cn.lambdalib2.render.legacy.GL11.*;

/**
 * Screen-space ripples. Ported from Scala.
 * The original distorted the screen with the vm_wave shader; this port draws the ripple texture blended instead.
 */
@Environment(EnvType.CLIENT)
public class WaveEffectUI {

    private static final Identifier TEXTURE = Resources.getTexture("effects/glow_circle");

    private static class Ripple {
        final float life;
        final float x, y;
        double timeAlive;
        final float size;

        Ripple(float life, float x, float y, double timeAlive, float size) {
            this.life = life;
            this.x = x;
            this.y = y;
            this.timeAlive = timeAlive;
            this.size = size;
        }

        float alpha() {
            double prog = timeAlive / life;
            if (prog < 0.2f) {
                return (float) (prog / 0.2f);
            } else if (prog < 0.5f) {
                return 1;
            } else {
                return (float) (1 - (prog - 0.5f) / 0.5f);
            }
        }

        float realSize() {
            return (float) (size + timeAlive * 20);
        }
    }

    public final float maxAlpha;
    public final float avgSize;
    public final float intensity;

    private double lastFrameTime = currentTime();

    private final LinkedList<Ripple> rippleList = new LinkedList<>();

    public WaveEffectUI(float maxAlpha, float avgSize, float intensity) {
        this.maxAlpha = maxAlpha;
        this.avgSize = avgSize;
        this.intensity = intensity;
    }

    public void onFrame(float width, float height) {
        double timeStamp = currentTime();
        double deltaTime = timeStamp - lastFrameTime;

        update(deltaTime, width, height);

        RenderUtils.pushTextureState();
        render();
        RenderUtils.popTextureState();

        lastFrameTime = timeStamp;
    }

    private void update(double deltaTime, float width, float height) {
        // Update existing ripples
        Iterator<Ripple> iter = rippleList.iterator();
        while (iter.hasNext()) {
            Ripple ripple = iter.next();
            ripple.timeAlive += deltaTime;
            if (ripple.timeAlive >= ripple.life) {
                iter.remove();
            }
        }

        // Spawn new ripples
        if (RandUtils.nextFloat() < deltaTime * intensity) {
            float rSize = RandUtils.rangef(0.8f, 1.2f) * avgSize;
            float rLife = RandUtils.rangef(1.5f, 2.5f);
            rippleList.add(new Ripple(rLife, RandUtils.nextFloat() * width, RandUtils.nextFloat() * height, 0, rSize));
        }
    }

    private void render() {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        HudUtils.loadTexture(TEXTURE);
        for (Ripple ripple : rippleList) {
            float sz = ripple.realSize();
            glColor4f(1, 1, 1, 0.3f * maxAlpha * ripple.alpha());
            HudUtils.rect(ripple.x - sz / 2, ripple.y - sz / 2, sz, sz);
        }
        glColor4f(1, 1, 1, 1);
    }

    private static double currentTime() {
        return GameTimer.getTime();
    }

}
