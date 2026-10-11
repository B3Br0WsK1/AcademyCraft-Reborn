/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of LambdaLib modding library.
* https://github.com/LambdaInnovation/LambdaLib
* Licensed under MIT, see project root for more information.
*/
package cn.lambdalib2.cgui.component;

import cn.lambdalib2.cgui.annotation.CGuiEditorComponent;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.render.gui.GuiDraw;
import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.Colors;

@CGuiEditorComponent
public class Tint extends Component {

    public Color
        idleColor,
        hoverColor;

    public boolean affectTexture = false;

    public double zLevel = 0.0;

    public Tint() {
        this(Colors.fromFloat(1, 1, 1, 0.6f), Colors.fromFloat(1, 1, 1, 1));
    }

    public Tint(Color idle, Color hover, boolean _affectTexture) {
        this(idle, hover);
        affectTexture = _affectTexture;
    }

    public Tint(Color idle, Color hover) {
        super("Tint");

        idleColor = idle;
        hoverColor = hover;

        listen(FrameEvent.class, (w, event) -> {
            if(affectTexture) {
                DrawTexture dt = w.getComponent(DrawTexture.class);
                if(dt != null) {
                    dt.color = event.hovering ? hoverColor : idleColor;
                }
            } else {
                GuiDraw.color(event.hovering ? hoverColor : idleColor);
                GuiDraw.colorRect(0, 0, w.transform.width, w.transform.height);
            }
        });
    }

    public Tint setAffectTexture() {
        affectTexture = true;
        return this;
    }

}
