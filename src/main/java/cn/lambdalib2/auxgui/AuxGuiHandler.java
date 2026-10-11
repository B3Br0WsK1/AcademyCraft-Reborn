package cn.lambdalib2.auxgui;

import cn.lambdalib2.LambdaLib2Client;
import cn.lambdalib2.event.EventHub;
import cn.lambdalib2.event.SubscribeEvent;
import cn.lambdalib2.event.mc.TickEvent;
import cn.lambdalib2.render.gui.GuiDraw;
import cn.lambdalib2.util.Debug;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.ScaledResolution;
import com.google.common.collect.ImmutableList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author WeathFolD
 */
@Environment(EnvType.CLIENT)
public class AuxGuiHandler {

    private static boolean initialized = false;

    private static boolean iterating;
    private static final List<AuxGui> auxGuiList = new LinkedList<>();
    private static final List<AuxGui> toAddList = new ArrayList<>();

    private AuxGuiHandler() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        EventHub.BUS.register(new AuxGuiHandler());
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("lambdalib2", "aux_gui"), AuxGuiHandler::doRender);
    }

    public static void register(AuxGui gui) {
        if(!iterating)
            doAdd(gui);
        else
            toAddList.add(gui);
    }

    public static List<AuxGui> active() {
        return ImmutableList.copyOf(auxGuiList);
    }

    public static boolean hasForegroundGui() {
        return auxGuiList.stream().anyMatch(gui -> !gui.disposed && gui.foreground);
    }

    private static void doAdd(AuxGui gui) {
        auxGuiList.add(gui);
        EventHub.BUS.post(new OpenAuxGuiEvent(gui));
        gui.onEnable();
    }

    private static void doRender(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        ScaledResolution res = new ScaledResolution(Minecraft.getInstance());
        GuiDraw.begin(graphics);
        try {
            Iterator<AuxGui> iter = auxGuiList.iterator();
            iterating = true;
            while(iter.hasNext()) {
                AuxGui gui = iter.next();
                if(!gui.disposed) {
                    if(!gui.lastFrameActive)
                        gui.lastActivateTime = GameTimer.getTime();
                    GuiDraw.resetState();
                    graphics.pose().pushMatrix();
                    try {
                        gui.draw(res);
                    } catch (RuntimeException e) {
                        Debug.error("Error drawing AuxGui " + gui, e);
                    }
                    graphics.pose().popMatrix();
                    gui.lastFrameActive = true;
                }
            }
        } finally {
            iterating = false;
            GuiDraw.end();
        }
    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if(!Minecraft.getInstance().isPaused()) {
            for(AuxGui gui : toAddList)
                doAdd(gui);
            toAddList.clear();

            Iterator<AuxGui> iter = auxGuiList.iterator();
            iterating = true;
            while(iter.hasNext()) {
                AuxGui gui = iter.next();

                if(gui.disposed) {
                    gui.onDisposed();
                    gui.lastFrameActive = false;
                    iter.remove();
                } else if(gui.requireTicking) {
                    if(!gui.lastFrameActive)
                        gui.lastActivateTime = GameTimer.getTime();
                    gui.onTick();
                    gui.lastFrameActive = true;
                }
            }
            iterating = false;
        }
    }

    @SubscribeEvent
    public void disconnected(cn.lambdalib2.event.mc.ClientDisconnectionEvent event) {
        iterating = true;
        Iterator<AuxGui> iter = auxGuiList.iterator();
        while(iter.hasNext()) {
            AuxGui gui = iter.next();
            if(!gui.consistent) {
                gui.onDisposed();
                iter.remove();
            }
        }
        iterating = false;
    }

}
