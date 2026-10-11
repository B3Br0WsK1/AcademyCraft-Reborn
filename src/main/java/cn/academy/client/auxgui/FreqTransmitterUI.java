package cn.academy.client.auxgui;
import cn.lambdalib2.render.legacy.GL11;
import cn.academy.AcademyCraft;
import cn.academy.client.render.util.ACRenderingHelper;
import cn.academy.Resources;
import cn.academy.energy.api.WirelessHelper;
import cn.academy.energy.api.block.IWirelessMatrix;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.api.block.IWirelessUser;
import cn.academy.event.energy.LinkUserEvent;
import cn.academy.event.energy.LinkNodeEvent;
import cn.academy.energy.impl.WirelessNet;
import cn.academy.terminal.app.AppFreqTransmitter;
import cn.lambdalib2.multiblock.BlockMulti;
import cn.lambdalib2.s11n.network.Future;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkS11nType;
import cn.lambdalib2.util.*;
import cn.lambdalib2.auxgui.AuxGui;
import cn.lambdalib2.render.font.IFont;
import cn.lambdalib2.render.font.IFont.Extent;
import cn.lambdalib2.render.font.IFont.FontOption;
import cn.lambdalib2.input.KeyManager;
import cn.academy.util.ACI18n;
import cn.lambdalib2.util.RayTraceResult;
import cn.lambdalib2.event.EventHub;
import cn.lambdalib2.event.SubscribeEvent;
import cn.lambdalib2.input.RawInputHooks;
import net.minecraft.world.level.block.Block;
import net.minecraft.client.Minecraft;
import cn.lambdalib2.util.ScaledResolution;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.util.StringUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import cn.lambdalib2.input.Keys;
import cn.lambdalib2.util.Color;
import net.fabricmc.api.EnvType;


/**
 * @author WeAthFolD
 */
public class FreqTransmitterUI extends AuxGui {
    /**
     * @author WeAthFolD
     */
    @NetworkS11nType
    static class Syncs {

        public static final String
            MSG_QUERY_SSID = "query_ssid",
            MSG_AUTH_MATRIX = "auth_matrix",
            MSG_AUTH_NODE   = "auth_node",
            MSG_LINK_NODE = "link_node",
            MSG_LINK_USER = "link_user";

        private static Object delegate = NetworkMessage.staticCaller(Syncs.class);

        public static void querySSID(IWirelessMatrix matrix, Future<String> future) {
            send(MSG_QUERY_SSID, matrix, future);
        }

        public static void authorizeMatrix(IWirelessMatrix matrix, String password, Future<Boolean> future) {
            send(MSG_AUTH_MATRIX, matrix, password, future);
        }

        public static void authorizeNode(IWirelessNode node, String password, Future<Boolean> future) {
            send(MSG_AUTH_NODE, node, password, future);
        }

        public static void linkNodeToMatrix(IWirelessNode node, IWirelessMatrix matrix, String password, Future<Boolean> future) {
            send(MSG_LINK_NODE, node, matrix, password, future);
        }

        public static void linkUserToNode(IWirelessUser user, IWirelessNode node, Future<Boolean> future) {
            send(MSG_LINK_USER, user, node, future);
        }

        @NetworkMessage.Listener(channel=MSG_QUERY_SSID, side=Side.SERVER)
        private static void hQuerySSID(IWirelessMatrix matrix, Future<String> future) {
            WirelessNet net = WirelessHelper.getWirelessNet(matrix);
            future.sendResult(net != null ? net.getSSID() : null);
        }

        @NetworkMessage.Listener(channel=MSG_AUTH_MATRIX, side=Side.SERVER)
        private static void hAuthorizeMatrix(IWirelessMatrix matrix, String password, Future<Boolean> future) {
            WirelessNet net = WirelessHelper.getWirelessNet(matrix);
            future.sendResult(net != null && net.getPassword().equals(password));
        }

        @NetworkMessage.Listener(channel=MSG_LINK_NODE, side=Side.SERVER)
        private static void hLinkNodeToMatrix(IWirelessNode node, IWirelessMatrix matrix, String password, Future<Boolean> future) {
            WirelessNet net = WirelessHelper.getWirelessNet(matrix);
            future.sendResult(net != null &&
                !EventHub.BUS.post(new LinkNodeEvent(node, net.getMatrix(), password)));
        }

        @NetworkMessage.Listener(channel=MSG_LINK_USER, side=Side.SERVER)
        private static void hLinkUserToNode(IWirelessUser user, IWirelessNode node, Future<Boolean> future) {
            future.sendResult(!EventHub.BUS.post(new LinkUserEvent(user, node)));
        }

        @NetworkMessage.Listener(channel=MSG_AUTH_NODE, side=Side.SERVER)
        private static void hAuthNode(IWirelessNode node, String pass, Future<Boolean> future) {
            future.sendResult(node.getPassword().equals(pass));
        }

        private static void send(String channel, Object... args) {
            NetworkMessage.sendToServer(delegate, channel, args);
        }

    }

    private static final String OVERRIDE_GROUP = "AC_FreqTransmitter";

    @Environment(EnvType.CLIENT)
    private abstract class State {
        
        boolean handlesKey;
        final double createTime;
        long timeout = 20000;
        
        public State(boolean _handlesKey) {
            handlesKey = _handlesKey;
            createTime = GameTimer.getTime();
        }
        
        final boolean handlesKeyInput() {
            return handlesKey;
        }
        
        abstract void handleDraw(float w, float h);
        abstract void handleClicking(RayTraceResult result);
        
        void handleKeyInput(char ch, int kid) {}
        
        final long getDeltaTime() {
            return (long) ((GameTimer.getTime() - createTime) * 1000);
        }

        final void startTransmitting() {
            timeout = 3000;
        }
    }
    
    private static final Color
        BG_COLOR = Colors.fromHexColor(0x77272727),
        GLOW_COLOR = Colors.fromHexColor(0xaaffffff);
    
    private static final double GLOW_SIZE = 1;

    final IFont font = Resources.font();
    
    Player player;
    Level world;
    
    State current;
    
    KeyEventDispatcher keyDispatcher;
    
    public FreqTransmitterUI() {
        player = Minecraft.getInstance().player;
        world = player.level();
        consistent = false;
        
        RawInputHooks.add(keyDispatcher = new KeyEventDispatcher());
        
        setState(new StateStart());

        ControlOverrider.override(OVERRIDE_GROUP, KeyManager.MOUSE_LEFT, KeyManager.MOUSE_RIGHT);
    }
    
    private void setState(State next) {
        if(next == null) {
            this.dispose();
            
            if(current.handlesKeyInput()) {
                ControlOverrider.endCompleteOverride();
            }
        } else {
            if(current != null && current.handlesKeyInput()) {
                ControlOverrider.endCompleteOverride();
            }
            if(next.handlesKeyInput()) {
                ControlOverrider.startCompleteOverride();
            }
        }
        current = next;
    }

    @Environment(EnvType.CLIENT)
    private String local(String key) {
        return ACI18n.translateFormatted("ac.app.freq_transmitter." + key);
    }
    
    @Override
    public void onDisposed() {
        keyDispatcher.discard();

        ControlOverrider.endOverride(OVERRIDE_GROUP);
    }

    @Override
    public void draw(ScaledResolution sr) {
        float width = (float) sr.getScaledWidth_double(), height = (float) sr.getScaledHeight_double();
        
        AppFreqTransmitter app = AppFreqTransmitter.instance;
        GL11.glPushMatrix(); {
            
            GL11.glTranslated(15, 15, 0);
            
            final float isize = 18;
            final FontOption option = new FontOption(10);

            String str = app.getDisplayName();
            double len = font.getTextWidth(str, option);
            
            drawBox(0, 0, 30 + len, 18);
            
            Identifier icon = app.getIcon();
            RenderUtils.loadTexture(icon);
            GL11.glColor4d(1, 1, 1, 1);
            HudUtils.rect(2, 0, isize, isize);
            
            font.draw(str, isize + 6, 4, option);
        
        } GL11.glPopMatrix();
        
        current.handleDraw(width, height);

        long dt = current.getDeltaTime();
        if (dt > current.timeout) {
            setState(new StateNotifyAndQuit("st"));
        }
        
        GL11.glColor4d(1, 1, 1, 1);
    }
    
    private static void drawBox(double x, double y, double width, double height) {
        Colors.bindToGL(BG_COLOR);
        HudUtils.colorRect(x, y, width, height);
        
        ACRenderingHelper.drawGlow(x, y, width, height, GLOW_SIZE, GLOW_COLOR);
    }
     
    private void drawTextBox(String str, float x, float y) {
        final float trimLength = 120;
        final FontOption option = new FontOption(10);
        Extent extent = font.drawSeperated_Sim(str, trimLength, option);
        final float X0 = x, Y0 = y, MARGIN = 5;
        
        drawBox(X0, Y0, MARGIN * 2 + extent.width + 25, MARGIN * 2 + extent.height);
        font.drawSeperated(str, X0 + MARGIN, Y0 + MARGIN, trimLength, option);
    }
    
    private class KeyEventDispatcher implements RawInputHooks.Listener {

        @Override
        public boolean onMouseButton(int button, boolean press) {
            if (current == null) {
                discard();
                return false;
            }
            if (button == 1 && press) {
                current.handleClicking(Raytrace.traceLiving(player, 4, EntitySelectors.nothing()));
                return true;
            }
            return false;
        }

        @Override
        public boolean onKey(int key, boolean press) {
            if (current != null && press && current.handlesKeyInput()) {
                if (key == Keys.KEY_RETURN || key == Keys.KEY_BACK) {
                    current.handleKeyInput(' ', key);
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean onChar(int codepoint) {
            if (current != null && current.handlesKeyInput()) {
                current.handleKeyInput((char) codepoint, -1);
                return true;
            }
            return false;
        }

        protected void discard() {
            RawInputHooks.remove(this);
        }

    }

    // STATES
    
    private class StateNotify extends State {
        
        final String key;
        
        public StateNotify(String _key) {
            super(false);
            key = _key;
        }
        
        @Override
        public void handleDraw(float w, float h) {
            drawTextBox(local(key), w / 2 + 10, h / 2 + 10);
        }

        @Override
        void handleClicking(RayTraceResult result) {}
        
    }
    
    private class StateNotifyAndQuit extends StateNotify {
        
        public StateNotifyAndQuit(String _key) {
            super(_key);
        }
        
        @Override
        public void handleDraw(float w, float h) {
            super.handleDraw(w, h);
            if(this.getDeltaTime() > 1000L) {
                dispose();
            }
        }
        
    }
    
    private class StateNotifyAndReturn extends StateNotify {
        final State toSwitch;
        
        public StateNotifyAndReturn(String _key, State _toSwitch) {
            super(_key);
            toSwitch = _toSwitch;
        }
        
        @Override
        public void handleDraw(float w, float h) {
            super.handleDraw(w, h);
            if(this.getDeltaTime() > 700L) {
                setState(toSwitch);
            }
        }
    }
    
    // S0
    private class StateStart extends State {
        
        boolean started = false;
        
        public StateStart() {
            super(false);
        }

        @Override
        public void handleDraw(float w, float h) {
            drawTextBox(local("s0_0"), w / 2 + 10, h / 2 + 10);
        }

        @Override
        public void handleClicking(RayTraceResult result) {
            if(result == null) {
                setState(null);
                return;
            }
            if(started)
                return;
//            int hx = result.blockX,
//                    hy = result.blockY,
//                    hz = result.blockZ;
            BlockEntity te = world.getBlockEntity(result.getBlockPos());
            if(te instanceof IWirelessNode) {
                
                setState(new StateAuthorizeNode((IWirelessNode) te));
                
            } else if(te instanceof IWirelessMatrix) {
                
                started = true;
                IWirelessMatrix mat = (IWirelessMatrix) te;
                // Hard coded BlockMulti processing
                Block block = world.getBlockState(result.getBlockPos()).getBlock();
                if(block instanceof BlockMulti) {
                    mat = (IWirelessMatrix) ((BlockMulti)block).getOriginTile(te);
                    if(mat == null) {
                        setState(new StateNotifyAndQuit("e0"));
                        return;
                    }
                }
                
                final IWirelessMatrix mat2 = mat;
                startTransmitting();
                Syncs.querySSID(mat, Future.create(ssid -> {
                    if(current == StateStart.this) {
                        if(ssid == null) {
                            setState(new StateNotifyAndQuit("e0"));
                        } else {
                            setState(new StateAuthorize(mat2, ssid));
                        }
                    }
                }));
            } else {
                setState(new StateNotifyAndQuit("e4"));
            }
        }
        
    }
    
    // S1
    private class StateAuthorize extends State {
        
        final IWirelessMatrix matrix;
        final String ssid;
        String pass = "";

        public StateAuthorize(IWirelessMatrix _matrix, String _ssid) {
            super(true);
            matrix = _matrix;
            ssid = _ssid;
        }

        @Override
        void handleDraw(float w, float h) {
            GL11.glPushMatrix();
            GL11.glTranslated(w / 2 + 10, h / 2 - 10, 0);
            
            drawBox(0, 0, 140, 40);
            
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < pass.length(); ++i)
                sb.append('*');
            
            font.draw(String.format("SSID: %s", ssid), 10, 5, new FontOption(10, 0xffbfbfbf));
            font.draw(String.format("PASS: %s", sb.toString()), 10, 15, new FontOption(10, 0xffffffff));
            font.draw(local("s1_0"), 10, 25, new FontOption(10, 0xff30ffff));
            GL11.glPopMatrix();
        }

        @Override
        void handleClicking(RayTraceResult result) {
            // NO-OP
        }
        
        @Override
        void handleKeyInput(char ch, int kid) {
            if((ch != 0 && StringUtil.isAllowedChatCharacter(ch))) {
                pass = pass + ch;
            } else if(kid == Keys.KEY_RETURN) {
                State state = new StateNotify("s1_1");
                setState(state);
                state.startTransmitting();
                Syncs.authorizeMatrix(matrix, pass, Future.create(result -> {
                    if(state == FreqTransmitterUI.this.current) {
                        if(result) {
                            setState(new StateDoMatrixLink(matrix, pass));
                        } else {
                            setState(new StateNotifyAndQuit("e1"));
                        }
                    }
                }));
            } else if(kid == Keys.KEY_BACK) {
                if(pass.length() > 0)
                    pass = pass.substring(0, pass.length() - 1);
            }
        }
        
    }

    private class StateAuthorizeNode extends State {

        final IWirelessNode node;
        final String name;
        String pass = "";

        public StateAuthorizeNode(IWirelessNode _node) {
            super(true);
            node = _node;
            name = node.getNodeName();
        }

        @Override
        void handleDraw(float w, float h) {
            GL11.glPushMatrix();
            GL11.glTranslated(w / 2 + 10, h / 2 - 10, 0);

            drawBox(0, 0, 140, 40);

            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < pass.length(); ++i)
                sb.append('*');

            font.draw(String.format("NAME: %s", name), 10, 5, new FontOption(10, 0xffbfbfbf));
            font.draw(String.format("PASS: %s", sb.toString()), 10, 15, new FontOption(10, 0xffffffff));
            font.draw(local("s1_1"), 10, 25, new FontOption(10, 0xff30ffff));
            GL11.glPopMatrix();
        }

        @Override
        void handleClicking(RayTraceResult result) {
            // NO-OP
        }

        @Override
        void handleKeyInput(char ch, int kid) {
            if((ch != 0 && StringUtil.isAllowedChatCharacter(ch))) {
                pass = pass + ch;
            } else if(kid == Keys.KEY_RETURN) {
                State state = new StateNotify("s1_1");
                setState(state);
                state.startTransmitting();
                Syncs.authorizeNode(node, pass, Future.create(result -> {
                    if(state == FreqTransmitterUI.this.current) {
                        if(result) {
                            setState(new StateDoNodeLink(node, pass));
                        } else {
                            setState(new StateNotifyAndQuit("e1"));
                        }
                    }
                }));
            } else if(kid == Keys.KEY_BACK) {
                if(pass.length() > 0)
                    pass = pass.substring(0, pass.length() - 1);
            }
        }

    }
    
    //S2
    private class StateDoMatrixLink extends State {

        final IWirelessMatrix matrix;
        final String pass;
        
        public StateDoMatrixLink(IWirelessMatrix _matrix, String _pass) {
            super(false);
            matrix = _matrix;
            pass = _pass;
        }

        @Override
        void handleDraw(float w, float h) {
            drawTextBox(local("s2_0"), w / 2 + 10, h / 2 + 10);
        }

        @Override
        void handleClicking(RayTraceResult result) {
            BlockEntity tile;
            
            if(result == null || 
                !((tile = world.getBlockEntity(result.getBlockPos())) instanceof IWirelessNode)) {
                setState(new StateNotifyAndQuit("e4"));
            } else {
                IWirelessNode node = (IWirelessNode) tile;
                State state = new StateNotify("e5");
                setState(state);
                state.startTransmitting();
                Syncs.linkNodeToMatrix(node, matrix, pass, Future.create(res -> {
                    if(FreqTransmitterUI.this.current == state) {
                        if(res) {
                            setState(new StateNotifyAndReturn("e6", StateDoMatrixLink.this));
                        } else {
                            setState(new StateNotifyAndQuit("e2"));
                        }
                    }
                }));
            }
        }
        
    }
    
    //S3
    private class StateDoNodeLink extends State {
        
        IWirelessNode node;
        String pass;

        public StateDoNodeLink(IWirelessNode _node, String _pass) {
            super(false);
            node = _node;
            pass = _pass;
        }

        @Override
        void handleDraw(float w, float h) {
            drawTextBox(local("s3_0"), w / 2 + 10, h / 2 + 10);
        }

        @Override
        void handleClicking(RayTraceResult r) {
            BlockEntity tile;
            if(r == null || (tile = world.getBlockEntity(r.getBlockPos())) == null) {
                setState(new StateNotifyAndQuit("e4"));
                return;
            }
            
            Block block = tile.getBlockState().getBlock();
            if(block instanceof BlockMulti) {
                tile = ((BlockMulti) block).getOriginTile(tile);
            }
            
            if(tile instanceof IWirelessUser) {
                State state = new StateNotify("e5");
                setState(state);
                state.startTransmitting();
                Syncs.linkUserToNode((IWirelessUser) tile, node, Future.create(res -> {
                    if(FreqTransmitterUI.this.current == state) {
                        if(res) {
                            setState(new StateNotifyAndReturn("e6", StateDoNodeLink.this));
                        } else {
                            setState(new StateNotifyAndQuit("e3"));
                        }
                    }
                }));
            } else {
                setState(new StateNotifyAndQuit("e4"));
            }
        }
        
    }


}