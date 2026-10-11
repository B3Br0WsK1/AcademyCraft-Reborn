package cn.academy.ability.vanilla.vecmanip.client.effect;

import cn.academy.ability.context.Context.Status;
import cn.academy.ability.vanilla.vecmanip.skill.StormWing;
import cn.academy.ability.vanilla.vecmanip.skill.StormWing.StormWingContext;
import cn.academy.entity.LocalEntity;
import cn.lambdalib2.vis.CompTransform;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

import static cn.lambdalib2.render.legacy.GL11.*;

/**
 * Ported from Scala.
 */
@Environment(EnvType.CLIENT)
public class StormWingEffect extends LocalEntity {

    public record SubEffect(TornadoEffect eff, CompTransform trans) {}

    public final StormWingContext ctx;
    public final List<SubEffect> tornadoList = new ArrayList<>();

    private final Player player;

    private static final int TERMINATE_TICK = 15;

    private boolean terminated = false;
    private int terminateTick = 0;

    public StormWingEffect(StormWingContext ctx) {
        super(ctx.player.level());
        this.ctx = ctx;
        this.player = ctx.player;

        for (int i = 0; i < 4; ++i) {
            tornadoList.add(new SubEffect(new TornadoEffect(2, 0.16, 1.0, 2.0), new CompTransform()));
        }

        int sep = 45;
        tornadoList.get(0).trans().setTransform(-0.1, -0.3, 0.1).setRotation(0, sep, sep);
        tornadoList.get(1).trans().setTransform(0.1, -0.3, 0.1).setRotation(0, -sep, -sep);
        tornadoList.get(2).trans().setTransform(-0.1, -0.5, -0.1).setRotation(0, -sep, sep);
        tornadoList.get(3).trans().setTransform(0.1, -0.5, -0.1).setRotation(0, sep, -sep);

        setRot(player.yBodyRot, player.getXRot());
        updatePosition();
    }

    @Override
    public void onUpdate() {
        if (ctx.getStatus() == Status.TERMINATED) {
            terminated = true;
        }

        if (terminated) {
            terminateTick += 1;

            if (terminateTick > TERMINATE_TICK) {
                setDead();
            }
        }

        updatePosition();
        setRot(player.yBodyRot, player.getXRot());

        double alpha;
        if (ctx.getState() == StormWing.STATE_CHARGE) {
            alpha = (double) ctx.getStateTick() / ctx.chargeTime * 0.7;
        } else if (!terminated) {
            alpha = 0.7;
        } else {
            alpha = 0.7 * (1 - (double) terminateTick / TERMINATE_TICK);
        }

        for (SubEffect s : tornadoList) s.eff().alpha = alpha;
    }

    private void updatePosition() {
        setPos(player.getX(), player.getY() + 1.6, player.getZ());
    }

    @Override
    public void renderEffect(double x, double y, double z, float partialTicks) {
        glDisable(GL_ALPHA_TEST);
        glPushMatrix();

        glTranslated(x, y, z);

        glRotated(-getYRot(), 0, 1, 0);
        glRotated(getXRot() * 0.2, 1, 0, 0);
        glRotated(-70, 1, 0, 0);

        glTranslated(0, 0.2, -0.5);

        for (SubEffect s : tornadoList) {
            glPushMatrix();

            s.trans().doTransform();
            TornadoRenderer.doRender(s.eff());

            glPopMatrix();
        }

        glPopMatrix();
        glEnable(GL_ALPHA_TEST);
    }

}
