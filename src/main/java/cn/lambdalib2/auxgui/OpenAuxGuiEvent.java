package cn.lambdalib2.auxgui;

import cn.lambdalib2.event.Event;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Fired whenever an new AuxGui is opened.
 */
@Environment(EnvType.CLIENT)
public class OpenAuxGuiEvent extends Event {

    public final AuxGui gui;

    public OpenAuxGuiEvent(AuxGui _gui) {
        gui = _gui;
    }

}
