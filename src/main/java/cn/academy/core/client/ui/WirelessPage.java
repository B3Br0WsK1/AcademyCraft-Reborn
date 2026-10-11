package cn.academy.core.client.ui;

import cn.academy.Resources;
import cn.academy.core.client.ui.TechUI.Page;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.api.block.IWirelessUser;
import cn.academy.util.LocalHelper;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.component.Component;
import cn.lambdalib2.cgui.component.DrawTexture;
import cn.lambdalib2.cgui.component.ElementList;
import cn.lambdalib2.cgui.component.TextBox;
import cn.lambdalib2.cgui.component.TextBox.ConfirmInputEvent;
import cn.lambdalib2.cgui.component.Tint;
import cn.lambdalib2.cgui.event.GainFocusEvent;
import cn.lambdalib2.cgui.event.LeftClickEvent;
import cn.lambdalib2.cgui.event.LostFocusEvent;
import cn.lambdalib2.s11n.network.Future;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.util.Colors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

import static cn.academy.core.client.ui.WirelessNetDelegate.*;

/**
 * The wireless connection page of machine GUIs. Ported from Scala.
 */
@Environment(EnvType.CLIENT)
public final class WirelessPage {

    private WirelessPage() {}

    private static Widget wirelessPageTemplate;

    private static Widget template() {
        if (wirelessPageTemplate == null) {
            wirelessPageTemplate = TechUI.readxml("page_wireless").getWidget("main");
        }
        return wirelessPageTemplate;
    }

    private static final Identifier connectedIcon = Resources.getTexture("guis/icons/icon_connected");
    private static final Identifier unconnectedIcon = Resources.getTexture("guis/icons/icon_unconnected");
    private static final Identifier toMatrixIcon = Resources.getTexture("guis/icons/icon_tomatrix");

    private static final LocalHelper local = TechUI.local.subPath("pg_wireless");

    public interface Target {
        String name();
    }

    public interface AvailTarget extends Target {
        void connect(String pass);
        boolean encrypted();
    }

    public interface LinkedTarget extends Target {
        void disconnect();
    }

    public static class LinkedInfo extends Component {
        public LinkedTarget target;

        public LinkedInfo(LinkedTarget target) {
            super("LinkedInfo");
            this.target = target;
        }

        @Override
        public Component copy() {
            return new LinkedInfo(target);
        }
    }

    private static void rebuildPage(Widget window, LinkedTarget linked, List<AvailTarget> avail) {
        Widget wlist = window.getWidget("panel_wireless/zone_elementlist");
        wlist.removeComponent(ElementList.class);

        ElementList elist = new ElementList();
        wlist.getWidget("element").transform.doesDraw = false;
        Widget elemTemplate = wlist.getWidget("element").copy();

        {
            Widget connectElem = window.getWidget("panel_wireless/elem_connected");

            Identifier icon;
            String name;
            float alpha;
            boolean tintEnabled;
            if (linked != null) {
                icon = connectedIcon;
                name = linked.name();
                alpha = 1.0f;
                tintEnabled = true;
            } else {
                icon = unconnectedIcon;
                name = local.get("not_connected");
                alpha = 0.6f;
                tintEnabled = false;
            }

            Widget iconConnect = connectElem.getWidget("icon_connect");
            iconConnect.getComponent(DrawTexture.class).texture = icon;
            iconConnect.getComponent(DrawTexture.class).color.setAlpha(Colors.f2i(alpha));
            iconConnect.getComponent(Tint.class).enabled = tintEnabled;
            connectElem.getWidget("icon_logo").getComponent(DrawTexture.class).color.setAlpha(Colors.f2i(alpha));
            connectElem.getWidget("text_name").getComponent(TextBox.class).setContent(name);

            iconConnect.getComponent(LinkedInfo.class).target = linked;
        }

        for (AvailTarget target : avail) {
            Widget instance = elemTemplate.copy();

            Widget passBox = instance.getWidget("input_pass");
            Widget iconKey = instance.getWidget("icon_key");

            Runnable confirm = () -> {
                String password = passBox.getComponent(TextBox.class).content;
                target.connect(password);

                passBox.getComponent(TextBox.class).setContent("");
            };

            instance.getWidget("text_name").getComponent(TextBox.class).setContent(target.name());

            if (target.encrypted()) {
                passBox.listen(ConfirmInputEvent.class, (w, e) -> confirm.run());
                passBox.listen(GainFocusEvent.class, (w, e) ->
                    iconKey.getComponent(DrawTexture.class).color.setAlpha(Colors.f2i(1.0f)));
                passBox.listen(LostFocusEvent.class, (w, e) ->
                    iconKey.getComponent(DrawTexture.class).color.setAlpha(Colors.f2i(0.6f)));
            } else {
                passBox.transform.doesDraw = false;
                iconKey.transform.doesDraw = false;
            }

            instance.getWidget("icon_connect").listen(LeftClickEvent.class, (w, e) -> confirm.run());

            elist.addWidget(instance);
        }

        wlist.addComponent(elist);
    }

    public static Page nodePage(BlockEntity node) {
        Page ret = create();
        rebuildNode(ret, node);
        ret.window().getWidget("icon_logo").getComponent(DrawTexture.class).setTex(toMatrixIcon);
        return ret;
    }

    private static void rebuildNode(Page ret, BlockEntity node) {
        Level world = node.getLevel();
        send(MSG_FIND_NETWORKS, node, Future.create((NodeResult data) -> {
            LinkedTarget linked = null;
            if (data.linked != null) {
                MatrixData matrix = data.linked;
                linked = new LinkedTarget() {
                    @Override
                    public void disconnect() {
                        send(MSG_NODE_DISCONNECT, node, Future.create((Boolean b) -> rebuildNode(ret, node)));
                    }

                    @Override
                    public String name() {
                        return matrix.ssid;
                    }
                };
            }
            List<AvailTarget> avail = new ArrayList<>();
            for (MatrixData matrix : data.avail) {
                BlockEntity tile = matrix.tile(world);
                if (tile == null) continue;
                avail.add(new AvailTarget() {
                    @Override
                    public void connect(String pass) {
                        send(MSG_NODE_CONNECT, node, tile, pass, Future.create((Boolean b) -> rebuildNode(ret, node)));
                    }

                    @Override
                    public String name() {
                        return matrix.ssid;
                    }

                    @Override
                    public boolean encrypted() {
                        return matrix.encrypted;
                    }
                });
            }

            rebuildPage(ret.window(), linked, avail);
        }));
    }

    public static Page userPage(BlockEntity user) {
        Page ret = create();
        rebuildUser(ret, user);
        return ret;
    }

    private static void rebuildUser(Page ret, BlockEntity user) {
        Level world = user.getLevel();
        send(MSG_FIND_NODES, user, Future.create((UserResult result) -> {
            LinkedTarget linked = null;
            if (result.linked != null) {
                BlockEntity tile = result.linked.tile(world);
                if (tile instanceof IWirelessNode node) {
                    linked = new LinkedTarget() {
                        @Override
                        public void disconnect() {
                            send(MSG_USER_DISCONNECT, user, Future.create((Boolean b) -> rebuildUser(ret, user)));
                        }

                        @Override
                        public String name() {
                            return node.getNodeName();
                        }
                    };
                }
            }

            List<AvailTarget> avail = new ArrayList<>();
            for (NodeData a : result.avail) {
                BlockEntity tile = a.tile(world);
                if (!(tile instanceof IWirelessNode node)) continue;
                boolean enc = a.encrypted;
                avail.add(new AvailTarget() {
                    @Override
                    public void connect(String pass) {
                        send(MSG_USER_CONNECT, user, tile, pass, Future.create((Boolean b) -> rebuildUser(ret, user)));
                    }

                    @Override
                    public String name() {
                        return node.getNodeName();
                    }

                    @Override
                    public boolean encrypted() {
                        return enc;
                    }
                });
            }

            rebuildPage(ret.window(), linked, avail);
        }));
    }

    private static Page create() {
        Widget widget = template().copy();

        TechUI.breathe(widget.getWidget("icon_logo"));

        Widget wirelessPanel = widget.getWidget("panel_wireless");

        Widget wlist = wirelessPanel.getWidget("zone_elementlist");

        wirelessPanel.getWidget("btn_arrowup").listen(LeftClickEvent.class, (w, e) -> {
            ElementList elist = wlist.getComponent(ElementList.class);
            if (elist != null) elist.progressLast();
        });
        wirelessPanel.getWidget("btn_arrowdown").listen(LeftClickEvent.class, (w, e) -> {
            ElementList elist = wlist.getComponent(ElementList.class);
            if (elist != null) elist.progressNext();
        });

        Widget connectIcon = widget.getWidget("panel_wireless/elem_connected/icon_connect");
        connectIcon.addComponent(new LinkedInfo(null));
        connectIcon.listen(LeftClickEvent.class, (w, e) -> {
            LinkedTarget target = connectIcon.getComponent(LinkedInfo.class).target;
            if (target != null) {
                target.disconnect();
            }
        });

        return new Page("wireless", widget);
    }

    private static void send(String msg, Object... pars) {
        NetworkMessage.sendToServer(WirelessNetDelegate.INSTANCE, msg, pars);
    }

}
