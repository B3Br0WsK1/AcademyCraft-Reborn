package cn.academy.client.auxgui;
import cn.academy.AcademyCraft;
import cn.lambdalib2.auxgui.AuxGui;
import cn.lambdalib2.cgui.CGui;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.registry.StateEventCallback;
import com.google.common.collect.ImmutableList;
import cn.lambdalib2.util.ScaledResolution;
import cn.lambdalib2.util.Configuration;
import cn.lambdalib2.util.Configuration.Property;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;



/**
 * AC global HUD drawing dispatcher.
 * @author WeAthFolD
 */
@Environment(EnvType.CLIENT)
public class ACHud extends AuxGui {
    
    public static ACHud instance = new ACHud();

    @StateEventCallback
    private static void init(StateEvents.Init ev) {
        AuxGui.register(instance);
    }
    
    private List<Node> nodes = new ArrayList<>();
    
    private CGui gui = new CGui();

    private ACHud() {
        foreground = false;
    }

    @Override
    public void draw(ScaledResolution sr) {
        gui.resize(sr.getScaledWidth(), sr.getScaledHeight());
        for(Node n : nodes) {
            n.w.transform.doesDraw = n.cond.shows();
        }
        
        gui.draw();
    }
    
    public void addElement(Widget w, Condition showCondition, String name, Widget preview) {
        Node node = new Node(w, showCondition, name, preview);
        nodes.add(node);
        node.updatePosition();

        gui.addWidget(w);
    }

    public List<Node> getNodes() {
        return ImmutableList.copyOf(nodes);
    }
    
    public interface Condition {
        boolean shows();
    }
    
    public class Node {
        final Widget w;
        final Condition cond;
        final String name;
        final float defaultX, defaultY;
        final Widget preview;
        
        public Node(Widget _w, Condition _cond, String _name, Widget _preview) {
            w = _w;
            cond = _cond;
            name = _name;
            defaultX = w.transform.x;
            defaultY = w.transform.y;
            preview = _preview;
        }

        public double[] getPosition() {
            return prop().getDoubleList();
        }

        public Widget getPreview() { return preview; }

        public String getName() { return name; }

        void updatePosition() {
            double[] pos = getPosition();
            w.pos((float) pos[0], (float) pos[1]);
            w.dirty = true;
        }

        public void setPosition(float newX, float newY) {
            Property prop = prop();
            prop.set(new double[] { newX, newY });
            updatePosition();
        }

        private Property prop() {
            return conf().get("gui", name, new double[] { defaultX, defaultY });
        }

        private Configuration conf() {
            return AcademyCraft.config;
        }
    }

}