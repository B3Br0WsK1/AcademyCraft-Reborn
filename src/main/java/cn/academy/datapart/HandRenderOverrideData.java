package cn.academy.datapart;
import cn.academy.client.render.util.IHandRenderer;
import cn.lambdalib2.datapart.DataPart;
import cn.lambdalib2.datapart.EntityData;
import cn.lambdalib2.datapart.RegDataPart;
import cn.lambdalib2.util.Debug;
import net.minecraft.world.entity.player.Player;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;


@RegDataPart(value=Player.class, side={Side.CLIENT})
@Environment(EnvType.CLIENT)
public class HandRenderOverrideData extends DataPart<Player> {

    public static HandRenderOverrideData get(Player p) {
        return EntityData.get(p).getPart(HandRenderOverrideData.class);
    }

    private IHandRenderer current = null;

    public void addInterrupt(IHandRenderer r) {
        current = r;
    }

    public void stopInterrupt(IHandRenderer r) {
        if (r == current)
            current = null;
    }

    public IHandRenderer getRenderer() {
        return Debug.assertNotNull(current);
    }

    public boolean isRendererPresent() {
        return current != null;
    }

}
