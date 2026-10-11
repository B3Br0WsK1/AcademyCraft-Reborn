package cn.academy.energy.client.ui;

import cn.academy.Resources;
import cn.academy.block.container.ContainerNode;
import cn.academy.block.tileentity.TileNode;
import cn.academy.core.client.ui.InventoryPage;
import cn.academy.core.client.ui.TechUI;
import cn.academy.core.client.ui.TechUI.ContainerUI;
import cn.academy.core.client.ui.TechUI.Page;
import cn.academy.core.client.ui.WirelessPage;
import cn.academy.energy.NodeNetworkProxy;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.s11n.network.Future;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.HudUtils;
import cn.lambdalib2.util.RenderUtils;
import cn.lambdalib2.render.legacy.GL11;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import static cn.academy.energy.NodeNetworkProxy.*;

@Environment(EnvType.CLIENT)
public final class GuiNode {

    private GuiNode() {}

    public static final int STATE_LINKED = 0;
    public static final int STATE_UNLINKED = 1;

    public static final int ALL_FRAMES = 10;

    public record State(int begin, int frames, long frameTime) {}

    static final State[] states = { new State(0, 8, 800), new State(8, 2, 3000) };

    static final Identifier animTexture = Resources.getTexture("guis/effect/effect_node");

    static final class StateContext {
        final State state;
        int frame;
        double lastChange = GameTimer.getTime();

        StateContext(State state) {
            this.state = state;
        }

        void updateAndDraw(double w, double h) {
            double time = GameTimer.getTime();
            long dt = (long) ((time - lastChange) * 1000);

            if (dt >= state.frameTime()) {
                lastChange = time;
                frame = (frame + 1) % state.frames();
            }

            int texFrame = state.begin() + frame;

            RenderUtils.loadTexture(animTexture);
            GL11.glColor4d(1, 1, 1, TechUI.breatheAlpha());
            HudUtils.rawRect(0, 0,
                0, (double) texFrame / ALL_FRAMES,
                w, h,
                1, 1.0 / ALL_FRAMES);
            GL11.glColor4d(1, 1, 1, 1);
        }
    }

    public static ContainerUI<ContainerNode> apply(ContainerNode container) {
        TileNode tile = container.tile;
        Player thePlayer = Minecraft.getInstance().player;

        int[] state = { STATE_UNLINKED };

        Page invPage = InventoryPage.create("node");

        {
            Widget animArea = new Widget().pos(42, 35.5f).size(186, 75).scale(0.5f);
            StateContext[] stateContext = { new StateContext(states[state[0]]) };

            animArea.listen(FrameEvent.class, (w, e) -> {
                State s = states[state[0]];
                if (stateContext[0].state != s) {
                    stateContext[0] = new StateContext(s);
                }

                stateContext[0].updateAndDraw(animArea.transform.width, animArea.transform.height);
            });

            invPage.window().addWidget(animArea);
        }

        Page wirelessPage = WirelessPage.nodePage(tile);

        ContainerUI<ContainerNode> ret = new ContainerUI<>(container, invPage, wirelessPage);

        {
            int[] load = { 1 };

            send(MSG_INIT, tile, Future.create((Integer cap) -> load[0] = cap));

            ret.infoPage
                .histogram(
                    TechUI.histEnergy(tile::getEnergy, tile.getMaxEnergy()),
                    TechUI.histCapacity(() -> load[0], tile::getCapacity))
                .seplineInfo()
                .property("range", tile::getRange)
                .property("owner", tile::getPlacerName);

            if (tile.getPlacerName().equals(thePlayer.getName().getString())) {
                ret.infoPage
                    .property("node_name", tile::getNodeName, newName -> send(MSG_RENAME, thePlayer, tile, newName))
                    .property("password", tile::getPassword, newPass -> send(MSG_CHANGE_PASS, thePlayer, tile, newPass),
                        true, true, null);
            } else {
                ret.infoPage
                    .property("node_name", tile::getNodeName);
            }
        }

        { // Update node status listener
            double[] time = { GameTimer.getTime() - 2 };
            ret.main.listen(FrameEvent.class, (w, e) -> {
                double dt = GameTimer.getTime() - time[0];
                if (dt > 2) {
                    send(MSG_QUERY_LINK, tile, Future.create((Boolean res) -> state[0] = res ? STATE_LINKED : STATE_UNLINKED));
                    time[0] = GameTimer.getTime();
                }
            });
        }

        return ret;
    }

    private static void send(String channel, Object... pars) {
        NetworkMessage.sendToServer(NodeNetworkProxy.INSTANCE, channel, pars);
    }

}
