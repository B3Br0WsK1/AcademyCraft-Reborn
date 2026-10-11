/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of LambdaLib modding library.
* https://github.com/LambdaInnovation/LambdaLib
* Licensed under MIT, see project root for more information.
*/
package cn.lambdalib2.cgui.component;

import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.annotation.CGuiEditorComponent;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.render.gui.GuiDraw;
import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.MathUtils;
import net.minecraft.resources.Identifier;

@CGuiEditorComponent
public class ProgressBar extends Component {

    public enum Direction { RIGHT, LEFT, UP, DOWN }

    public boolean illustrating = false;

    public Identifier texture;

    public Direction dir = Direction.RIGHT;

    public double progress;

    public Color color = Colors.white();

    public ProgressBar() {
        super("ProgressBar");

        listen(FrameEvent.class, (wi, e) -> {
            if(illustrating) {
                progress = 0.5 * (1 + Math.sin(GameTimer.getAbsTime()));
            }

            double disp = MathUtils.clampd(0, 1, progress);

            double x, y, u, v, w, h, tw, th;
            double width = wi.transform.width, height = wi.transform.height;
            switch(dir) {
            case RIGHT:
                w = width * disp;
                h = height;
                x = y = 0;
                u = 0;
                v = 0;
                tw = disp;
                th = 1;
                break;
            case LEFT:
                w = width * disp;
                h = height;
                x = width - w;
                y = 0;
                u = (1 - disp);
                v = 0;
                tw = disp;
                th = 1;
                break;
            case UP:
                w = width;
                h = height * disp;
                x = 0;
                y = height * (1 - disp);
                u = 0;
                v = (1 - disp);
                tw = 1;
                th = disp;
                break;
            case DOWN:
                w = width;
                h = height * disp;
                x = y = 0;
                u = 0;
                v = 0;
                tw = 1;
                th = disp;
                break;
            default:
                throw new RuntimeException("niconiconi, WTF??");
            }

            if(texture != null && !texture.getPath().equals("<null>")) {
                GuiDraw.bindTexture(texture);
            } else {
                GuiDraw.disableTexture();
            }
            GuiDraw.color(color);
            GuiDraw.rawRect(x, y, u, v, w, h, tw, th);
            GuiDraw.enableTexture();
        });
    }

    public ProgressBar setDirection(Direction dir) {
        this.dir = dir;
        return this;
    }

    public static ProgressBar get(Widget w) {
        return w.getComponent(ProgressBar.class);
    }

}
