package cn.lambdalib2.particle;

import cn.lambdalib2.render.legacy.LegacyGL;
import cn.lambdalib2.render.legacy.Tessellator;
import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.Colors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;

import static cn.lambdalib2.render.legacy.GL11.*;

/**
 * Represents a drawable sprite in origin. Always face (0, 0, -1).
 *
 * @author WeAthFolD
 */
@Environment(EnvType.CLIENT)
public final class Sprite {

    /**
     * If the texture is null draw pure-colored sprite.
     */
    public Identifier texture;
    public float width = 1.0f, height = 1.0f;
    public Color color = Colors.white();
    public boolean hasLight = false;
    public boolean cullFace = true;

    public Sprite() {
    }

    public Sprite(Identifier rl) {
        texture = rl;
    }

    public Sprite setTexture(Identifier rl) {
        texture = rl;
        return this;
    }

    public Sprite setSize(float w, float h) {
        width = w;
        height = h;
        return this;
    }

    public Sprite enableLight() {
        hasLight = true;
        return this;
    }

    public Sprite disableCullFace() {
        cullFace = false;
        return this;
    }

    public Sprite setColor(Color nc) {
        color = nc;
        return this;
    }

    public void draw() {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        if (texture != null) {
            LegacyGL.bindTexture(texture);
            glEnable(GL_TEXTURE_2D);
        } else {
            glDisable(GL_TEXTURE_2D);
        }

        glDepthMask(false);
        LegacyGL.color(color.getRedF(), color.getGreenF(), color.getBlueF(), color.getAlphaF());
        Tessellator t = Tessellator.instance;
        float hw = width / 2, hh = height / 2;

        t.startDrawingQuads();
        t.addVertexWithUV(-hw, hh, 0, 0, 0);
        t.addVertexWithUV(-hw, -hh, 0, 0, 1);
        t.addVertexWithUV(hw, -hh, 0, 1, 1);
        t.addVertexWithUV(hw, hh, 0, 1, 0);
        t.draw();

        glDepthMask(true);
        glEnable(GL_TEXTURE_2D);
    }

}
