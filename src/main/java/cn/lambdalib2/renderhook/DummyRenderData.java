package cn.lambdalib2.renderhook;

import cn.lambdalib2.LLEntities;
import cn.lambdalib2.datapart.DataPart;
import cn.lambdalib2.datapart.EntityData;
import cn.lambdalib2.datapart.RegDataPart;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedList;
import java.util.List;

/**
 * @author WeAthFolD
 */
@Environment(EnvType.CLIENT)
@RegDataPart(value=Player.class, side=Side.CLIENT)
public class DummyRenderData extends DataPart<Player> {

    public DummyRenderData() {
        setTick(true);
    }

    public static DummyRenderData get(Player p) {
        return EntityData.get(p).getPart(DummyRenderData.class);
    }

    private EntityDummy entity;
    List<PlayerRenderHook> renderers = new LinkedList<>();

    @Override
    public void tick() {
        if(entity != null) {
            entity.player = getEntity();
        }

        renderers.removeIf(val -> val.disposed);

        // Destroy the entity when no more needed, saving resources
        if(entity != null && renderers.size() == 0) {
            entity.discard();
            entity = null;
        }
    }

    public void addRenderHook(PlayerRenderHook hook) {
        Player player = getEntity();
        hook.player = player;
        hook.disposed = false;

        if(entity == null || entity.isRemoved()) {
            entity = new EntityDummy(LLEntities.DUMMY, player.level());
            entity.init(this);
            player.level().addFreshEntity(entity);
        }

        renderers.add(hook);
    }

    List<PlayerRenderHook> getRenderers() {
        return renderers;
    }

}
