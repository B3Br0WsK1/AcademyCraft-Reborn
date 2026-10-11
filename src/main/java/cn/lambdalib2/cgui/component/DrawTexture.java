package cn.lambdalib2.cgui.component;

import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.annotation.CGuiEditorComponent;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.render.gui.GuiDraw;
import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.Colors;
import net.minecraft.resources.Identifier;

@CGuiEditorComponent
public class DrawTexture extends Component {

    public static final Identifier MISSING = Identifier.parse("lambdalib2:textures/cgui/missing.png");

    public enum DepthTestMode { Default, Equals }

    public Identifier texture;

    public Color color;

    public double zLevel = 0;

    public boolean writeDepth = true;

    public boolean doesUseUV;

    public double u = 0, v = 0, texWidth = 0, texHeight = 0;

    public DepthTestMode depthTestMode = DepthTestMode.Default;

    /**
     * Whether this texture is drawn with additive blending.
     */
    public boolean additive = false;

    public DrawTexture() {
        this(MISSING);
    }

    public DrawTexture(Identifier texture) {
        this(texture, Colors.white());
    }

    public DrawTexture(String name, Identifier _texture, Color _color) {
        super(name);
        this.texture = _texture;
        this.color = _color;

        listen(FrameEvent.class, (w, e) -> {
            GuiDraw.color(color);
            GuiDraw.setBlend(additive ? GuiDraw.Blend.ADDITIVE : GuiDraw.Blend.NORMAL);
            if(texture != null && !texture.getPath().equals("<null>")) {
                GuiDraw.bindTexture(texture);
                if (doesUseUV) {
                    GuiDraw.rect(0, 0, u, v, w.transform.width, w.transform.height, texWidth, texHeight);
                } else {
                    GuiDraw.rect(0, 0, w.transform.width, w.transform.height);
                }
            } else {
                GuiDraw.colorRect(0, 0, w.transform.width, w.transform.height);
            }
            GuiDraw.setBlend(GuiDraw.Blend.NORMAL);
        });
    }

    public DrawTexture(Identifier _texture, Color _color) {
        this("DrawTexture", _texture, _color);
    }

    public void setShaderId(int id) {
        // Custom shader programs are not supported by the 26.x render pipeline.
    }

    public DrawTexture setTex(Identifier t) {
        texture = t;
        return this;
    }

    public DrawTexture setUVRect(double u, double v, double texWidth, double texHeight) {
        doesUseUV = true;

        this.u = u;
        this.v = v;
        this.texWidth = texWidth;
        this.texHeight = texHeight;

        return this;
    }

    /**
     * Set the color as a **copy** of the given color.
     */
    public DrawTexture setColor(Color c) {
        this.color.setColor(c);
        return this;
    }

    public static DrawTexture get(Widget w) {
        return w.getComponent(DrawTexture.class);
    }

}
