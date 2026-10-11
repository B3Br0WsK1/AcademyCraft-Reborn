/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of LambdaLib modding library.
* https://github.com/LambdaInnovation/LambdaLib
* Licensed under MIT, see project root for more information.
*/
package cn.lambdalib2.cgui.event;

import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Handler interface of <code>GuiEvent</code>, typically registered in a {@link cn.lambdalib2.cgui.event.GuiEventBus}.
 */
@FunctionalInterface
@Environment(EnvType.CLIENT)
public interface IGuiEventHandler<T extends GuiEvent> {

    void handleEvent(Widget w, T event);
    
}
