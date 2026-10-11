package cn.academy.client.render.entity;
import cn.lambdalib2.render.legacy.GL11;
import cn.academy.Resources;
import cn.academy.entity.EntityCoinThrowing;
import cn.lambdalib2.util.RenderUtils;
import cn.lambdalib2.util.GameTimer;
import net.minecraft.client.Minecraft;
import cn.lambdalib2.render.legacy.LegacyRender;
import cn.lambdalib2.render.legacy.RenderManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;


/**
 * 
 * @author KSkun
 */
public class RendererCoinThrowing extends LegacyRender {

    public RendererCoinThrowing(RenderManager manager) {
        super(manager);
    }

    @Override
    public void doRender(Entity var1, double x, double y, double z,
            float var8, float var9) {
        EntityCoinThrowing etc = (EntityCoinThrowing) var1;
        Player player = etc.player;
        boolean fp = player == Minecraft.getInstance().player
                && Minecraft.getInstance().options.getCameraType().isFirstPerson();
        
        double dt = (GameTimer.getTime() * 1000) % 150;
        
        if(etc.player == null)
            return;
        //If syncedSingle and in client computer, do not render
        if(etc.isSync && player == Minecraft.getInstance().player)
            return;
        if(etc.getY() < player.getY())
            return;
        GL11.glPushMatrix(); {
            //x = player.getX() - RenderManager.renderPosX;
            //y = etc.getY() - RenderManager.renderPosY;
            //z = player.getZ() - RenderManager.renderPosZ;
            // Follow the (interpolated) player horizontally; the coin's own position lags a tick behind.
            // The original zeroed x/z for the local player, which only works in first person.
            x += net.minecraft.util.Mth.lerp(var9, player.xo, player.getX()) - net.minecraft.util.Mth.lerp(var9, etc.xo, etc.getX());
            z += net.minecraft.util.Mth.lerp(var9, player.zo, player.getZ()) - net.minecraft.util.Mth.lerp(var9, etc.zo, etc.getZ());
            
            GL11.glTranslated(x, y, z);
            if(fp) {
                GL11.glRotated(player.getYRot(), 0, -1, 0);
            } else GL11.glRotated(net.minecraft.util.Mth.rotLerp(var9, player.yBodyRotO, player.yBodyRot), 0, -1, 0);
            GL11.glTranslated(-0.63, 1, 0.30);
            float scale = 0.3F;
            GL11.glScalef(scale, scale, scale);
            GL11.glTranslated(0.5, 0.5, 0);
            GL11.glRotated((dt * 360.0 / 300.0), etc.axis.x, etc.axis.y, etc.axis.z);
            GL11.glTranslated(-0.5, -0.5, 0);
            RenderUtils.drawEquippedItem(0.0625, Resources.TEX_COIN_FRONT, Resources.TEX_COIN_BACK);
        } GL11.glPopMatrix();
    }
    
    @Override
    protected Identifier getEntityTexture(Entity var1) {
        return null;
    }

}