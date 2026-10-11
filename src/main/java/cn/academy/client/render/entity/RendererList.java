package cn.academy.client.render.entity;
import cn.lambdalib2.render.legacy.LegacyRender;
import cn.lambdalib2.render.legacy.RenderManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.Identifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



/**
 * @author WeAthFolD
 *
 */
public class RendererList<T extends Entity> extends LegacyRender<T> {
    
    List<LegacyRender> renderers = new ArrayList<>();
    
    public RendererList(RenderManager rm, LegacyRender ...rs) {
        super(rm);
        renderers.addAll(Arrays.asList(rs));
    }
    
    public RendererList append(LegacyRender e) {
        renderers.add(e);
        return this;
    }

    @Override
    public void doRender(T ent, double x,
            double y, double z, float a, float b) {
        for(LegacyRender r : renderers)
            r.doRender(ent, x, y, z, a, b);
    }

    @Override
    protected Identifier getEntityTexture(T entity) {
        return null;
    }

}