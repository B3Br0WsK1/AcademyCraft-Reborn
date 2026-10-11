package cn.academy.ability.vanilla.teleporter.skill;

import cn.academy.Resources;
import cn.academy.ability.AbilityContext;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.KeyDelegate;
import cn.academy.ability.vanilla.teleporter.util.TPSkillHelper;
import cn.academy.client.sound.ACSounds;
import cn.academy.datapart.AbilityData;
import cn.academy.util.ACI18n;
import cn.lambdalib2.cgui.CGuiScreen;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.component.Component;
import cn.lambdalib2.cgui.component.DrawTexture;
import cn.lambdalib2.cgui.component.ElementList;
import cn.lambdalib2.cgui.component.TextBox;
import cn.lambdalib2.cgui.component.TextBox.ConfirmInputEvent;
import cn.lambdalib2.cgui.component.Tint;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.cgui.event.LeftClickEvent;
import cn.lambdalib2.cgui.loader.CGUIDocument;
import cn.lambdalib2.datapart.DataPart;
import cn.lambdalib2.datapart.EntityData;
import cn.lambdalib2.datapart.RegDataPart;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.render.font.IFont;
import cn.lambdalib2.render.font.IFont.FontAlign;
import cn.lambdalib2.render.font.IFont.FontOption;
import cn.lambdalib2.s11n.SerializeIncluded;
import cn.lambdalib2.s11n.SerializeStrategy;
import cn.lambdalib2.s11n.SerializeStrategy.ExposeStrategy;
import cn.lambdalib2.s11n.nbt.NBTS11n;
import cn.lambdalib2.s11n.network.Future;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.NetworkS11n;
import cn.lambdalib2.s11n.network.NetworkS11nType;
import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.EntitySelectors;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.HudUtils;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.Side;
import cn.lambdalib2.util.WorldUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Location teleport. Ported from Scala.
 * Dimensions are stored by their registry id (e.g. "minecraft:the_nether") instead of the 1.12 integer ids.
 * @author WeAthFolD, KSkun
 */
public class LocationTeleport extends Skill {

    public static final LocationTeleport INSTANCE = new LocationTeleport();

    public static final Predicate<Entity> teleportSelector = EntitySelectors.living().and(
        t -> t.getBbWidth() * t.getBbWidth() * t.getBbHeight() < 80f);

    private LocationTeleport() {
        super("location_teleport", 3);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        rt.addKey(keyID, new KeyDelegate() {
            @Override
            public Identifier getIcon() {
                return getHintIcon();
            }

            @Override
            public int createID() {
                return 0;
            }

            @Override
            public Skill getSkill() {
                return INSTANCE;
            }

            @Override
            public void onKeyDown() {
                Minecraft.getInstance().gui.setScreen(new Gui());
            }
        });
    }

    public boolean canCrossDimension(Player player) {
        return AbilityData.get(player).getSkillExp(this) > 0.8f;
    }

    static String dimId(Level level) {
        return level.dimension().identifier().toString();
    }

    /**
     * @return {overload, cp} consumption
     */
    public float[] getConsumption(Player player, Location dest) {
        AbilityData data = AbilityData.get(player);
        float distance = (float) Math.sqrt(player.distanceToSqr(dest.x, dest.y, dest.z));
        int dimPenalty = isCrossDim(player, dest) ? 2 : 1;

        return new float[] { 240, MathUtils.lerpf(200, 150, data.getSkillExp(this)) * dimPenalty *
            Math.max(8.0f, Mth.sqrt(Math.min(800, distance))) };
    }

    /**
     * @return `null` if can perform, the reason otherwise.
     */
    public String getPerformStat(Player player, Location dest) {
        if (isCrossDim(player, dest) && !canCrossDimension(player)) {
            return ACI18n.format("ac.gui.loctele.err_exp");
        } else {
            AbilityContext ctx = AbilityContext.of(player, this);
            float cp = getConsumption(player, dest)[1];
            if (ctx.canConsumeCP(cp)) {
                return null;
            } else {
                return ACI18n.format("ac.gui.loctele.err_cp");
            }
        }
    }

    public void perform(Player player, Location dest) {
        AbilityContext ctx = AbilityContext.of(player, this);

        float[] cons = getConsumption(player, dest);
        ctx.consumeWithForce(cons[0], cons[1]);

        List<Entity> entitiesToTeleport = new ArrayList<>();
        entitiesToTeleport.add(player);
        entitiesToTeleport.addAll(WorldUtils.getEntities(player, 5,
            teleportSelector.and(EntitySelectors.exclude(player))));

        ServerLevel target = null;
        if (player.level() instanceof ServerLevel sl) {
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, Identifier.parse(dest.dim));
            target = sl.getServer().getLevel(key);
        }
        if (target == null) return;

        double dist = Math.sqrt(player.distanceToSqr(dest.x, dest.y, dest.z));
        float expincr = dist >= 200 ? 0.03f : 0.015f;
        double px = player.getX(), py = player.getY(), pz = player.getZ();
        for (Entity e : entitiesToTeleport) {
            double dx = e.getX() - px, dy = e.getY() - py, dz = e.getZ() - pz;
            if (e.isPassenger()) e.stopRiding();
            e.teleportTo(target, dest.x + dx, dest.y + dy, dest.z + dz, Set.of(), e.getYRot(), e.getXRot(), true);
        }

        ctx.addSkillExp(expincr);
        ctx.setCooldown((int) MathUtils.lerpf(30, 20, ctx.getSkillExp()));

        TPSkillHelper.incrTPCount(player);
    }

    private boolean isCrossDim(Player player, Location dest) {
        return !dimId(player.level()).equals(dest.dim);
    }

    // ---- Network ----

    public enum LTNetDelegate {
        INSTANCE;

        public static final String MSG_ADD = "add";
        public static final String MSG_REMOVE = "remove";
        public static final String MSG_PERFORM = "perform";

        @StateEventCallback
        private static void _init(StateEvents.Init ev) {
            NetworkS11n.register(ArrayList.class);
            NetworkS11n.addDirectInstance(LTNetDelegate.INSTANCE);
        }

        public static void send(String channel, Object... args) {
            NetworkMessage.sendToServer(LTNetDelegate.INSTANCE, channel, args);
        }

        @Listener(channel=MSG_ADD, side=Side.SERVER)
        private void hAdd(Player player, String name, Future<List<Location>> future) {
            LocTeleportData data = LocTeleportData.get(player);
            data.add(name, dimId(player.level()), (float) player.getX(), (float) player.getY(), (float) player.getZ());
            future.sendResult(new ArrayList<>(data.locations()));
        }

        @Listener(channel=MSG_REMOVE, side=Side.SERVER)
        private void hRemove(Player player, int id, Future<List<Location>> future) {
            LocTeleportData data = LocTeleportData.get(player);
            data.remove(id);
            future.sendResult(new ArrayList<>(data.locations()));
        }

        @Listener(channel=MSG_PERFORM, side=Side.SERVER)
        private void hPerform(Player player, Location location) {
            LocationTeleport.INSTANCE.perform(player, location);
        }
    }

    // ---- GUI ----

    @Environment(EnvType.CLIENT)
    static class GuiRes {
        static Widget template;

        static Widget template() {
            if (template == null) template = CGUIDocument.read(Resources.getGui("loctele_new")).getWidget("root");
            return template;
        }

        static final double ElemTimeStep = 0.06;

        static final float AlphaNormal = 0.1f;
        static final float AlphaHighlight = 0.4f;

        static final Color TextNormal = c(0xffc1cfd5);
        static final Color TextHighlight = c(0xff2e3b41);
        static final Color TextDisabled = c(0xffa2a2a2);

        private static Color c(int hex) {
            int a = (hex >>> 24) & 0xff;
            int r = (hex >> 16) & 0xff;
            int g = (hex >> 8) & 0xff;
            int b = hex & 0xff;
            return new Color(r, g, b, a);
        }

        static String dimensionName(String dim) {
            Identifier id = Identifier.parse(dim);
            String key = "dimension." + id.getNamespace() + "." + id.getPath();
            return ACI18n.canTranslate(key) ? ACI18n.translate(key) : id.getPath();
        }
    }

    @Environment(EnvType.CLIENT)
    static class Blend {
        private final double initTime = GameTimer.getTime();
        private final double timeOffset, length;

        Blend(double timeOffset, double length) {
            this.timeOffset = timeOffset;
            this.length = length;
        }

        float alpha() {
            double dt = (GameTimer.getTime() - initTime) - timeOffset;
            return (float) MathUtils.clampd(0, 1, dt / length);
        }
    }

    @Environment(EnvType.CLIENT)
    static class MessageTab extends Component {

        static final int textSize = 40;
        static final int lineHeight = 42;
        static final int ymargin = 20;
        static final int xmargin = 20;

        final FontOption fontOption = new FontOption(textSize, FontAlign.RIGHT, GuiRes.TextNormal);
        final IFont font = Resources.font();

        List<String> text = Collections.emptyList();

        MessageTab() {
            super("MessageTab");
            listen(FrameEvent.class, (w, evt) -> {
                for (int idx = 0; idx < text.size(); ++idx) {
                    float y = ymargin + lineHeight * idx;
                    font.draw(text.get(idx), widget.transform.width - xmargin, y, fontOption);
                }
            });
        }

        void updateText(List<String> t) {
            text = t;

            if (t.isEmpty()) {
                widget.transform.doesDraw = false;
            } else {
                widget.transform.doesDraw = true;
                float width = 0;
                for (String s : text) width = Math.max(width, font.getTextWidth(s, fontOption));
                width += xmargin * 2;
                float height = text.size() * lineHeight + ymargin * 2;

                widget.size(width, height);
            }
        }
    }

    @Environment(EnvType.CLIENT)
    interface HintMessage {
        double ypos();
        List<String> message();
        /**
         * @return Whether this message is available. Non-available message will be dropped.
         */
        boolean available();
    }

    @Environment(EnvType.CLIENT)
    public static class Gui extends CGuiScreen {

        final Widget root = GuiRes.template().copy();
        final Widget info = root.getWidget("info");
        final Widget list = root.getWidget("menu/list");

        final Player player = Minecraft.getInstance().player;
        final LocTeleportData data = LocTeleportData.get(player);

        HintMessage currentMessage = null;

        public Gui() {
            // hide templates
            list.getWidget("elem_template").transform.doesDraw = false;
            list.getWidget("add_template").transform.doesDraw = false;

            { // blend in menu
                Widget menu = root.getWidget("menu");
                Blend blend = new Blend(0, 0.4);
                float maxHeight = menu.transform.height;
                menu.transform.height = 0;
                menu.listen(FrameEvent.class, () -> menu.transform.height = blend.alpha() * maxHeight);
            }

            { // Initialize info area
                info.addComponent(new MessageTab());
                info.getComponent(MessageTab.class).updateText(Collections.emptyList());

                info.listen(FrameEvent.class, () -> {
                    if (currentMessage != null && !currentMessage.available()) setMessage(null);
                });
            }

            updateList(data.locations());

            getGui().addWidget(root);
        }

        private void wrapBack(Widget ret, int n, Supplier<List<String>> msg) {
            ret.removeComponent(Tint.class);

            Blend blend = new Blend(n * GuiRes.ElemTimeStep, 0.2);
            boolean[] lastHovering = { false };

            ret.listen(FrameEvent.class, () -> {
                boolean hovering = ret.isPointWithin(ret.getGui().getMouseX(), ret.getGui().getMouseY());

                if (!lastHovering[0] && hovering) {
                    lastHovering[0] = true;
                    double ypos = ret.y;
                    List<String> message = msg.get();
                    setMessage(new HintMessage() {
                        @Override public double ypos() { return ypos; }
                        @Override public List<String> message() { return message; }
                        @Override public boolean available() { return lastHovering[0]; }
                    });
                }

                float alpha0 = blend.alpha() * (hovering ? GuiRes.AlphaHighlight : GuiRes.AlphaNormal);
                Colors.bindToGL(Colors.whiteBlend(alpha0));
                HudUtils.colorRect(0, 0, ret.transform.width, ret.transform.height);

                lastHovering[0] = hovering;
            });
        }

        private void wrapButton(Widget target, int n, double offset, Runnable clickCallback) {
            Color color = new Color(GuiRes.TextNormal);
            color.setAlpha(0);

            target.getComponent(DrawTexture.class).color = color;
            Blend blend = new Blend(n * GuiRes.ElemTimeStep + offset, 0.1);
            target.listen(FrameEvent.class, (w, evt) -> {
                double a0 = evt.hovering ? 1.0 : 0.7;
                color.setAlpha((int) (a0 * blend.alpha() * 255));
            });
            target.listen(LeftClickEvent.class, clickCallback);
        }

        private void setMessage(HintMessage value) {
            double ypos = value == null ? 0.0 : value.ypos();
            List<String> texts = value == null ? Collections.emptyList() : value.message();

            currentMessage = value;
            getGui().moveWidgetToAbsPos(info, info.x, (float) ypos);
            info.getComponent(MessageTab.class).updateText(texts);
            getGui().updateWidget(info);
        }

        private void updateList(List<Location> locations) {
            list.removeComponent(ElementList.class);

            ElementList compList = new ElementList();
            compList.spacing = 2;

            for (Location l : locations) {
                compList.addWidget(newElem(l, compList.getSubWidgets().size()));
            }

            compList.addWidget(newAdd(compList.getSubWidgets().size()));

            list.addComponent(compList);
        }

        private Widget newElem(Location location, int count) {
            Widget ret = list.getWidget("elem_template").copy();
            ret.transform.doesDraw = true;

            TextBox textBox = ret.getWidget("text").getComponent(TextBox.class);
            textBox.setContent(location.name);

            String stat = INSTANCE.getPerformStat(player, location);
            float cp = INSTANCE.getConsumption(player, location)[1];

            wrapBack(ret, count, () -> {
                List<String> result = new ArrayList<>();
                result.add(GuiRes.dimensionName(location.dim) + " (" + location.dim + ")");
                result.add(String.format("(%.0f, %.0f, %.0f)", location.x, location.y, location.z));
                result.add(String.format("%.0f CP", cp));
                if (stat != null) result.add(stat);
                return result;
            });

            if (stat == null) {
                wrapButton(ret.getWidget("btn_teleport"), count, 0.03, () -> {
                    Minecraft.getInstance().gui.setScreen(null);

                    ACSounds.playClient(player, "tp.tp", SoundSource.AMBIENT, .5f);
                    LTNetDelegate.send(LTNetDelegate.MSG_PERFORM, player, location);
                });
            } else {
                ret.removeWidget("btn_teleport");
                ret.getWidget("text").getComponent(TextBox.class).option.color.set(0xa2, 0xa2, 0xa2);
            }

            wrapButton(ret.getWidget("btn_remove"), count, 0.05, () ->
                LTNetDelegate.send(LTNetDelegate.MSG_REMOVE, player, location.id,
                    Future.<List<Location>>create(this::updateList)));

            {
                Widget wid = ret.getWidget("text");
                TextBox text = wid.getComponent(TextBox.class);
                text.option.color.setAlpha(0);

                Blend blend = new Blend(count * GuiRes.ElemTimeStep + 0.1, 0.1);
                wid.listen(FrameEvent.class, () -> text.option.color.setAlpha((int) (blend.alpha() * 255)));
            }

            return ret;
        }

        private Widget newAdd(int count) {
            Widget ret = list.getWidget("add_template").copy();

            wrapBack(ret, count, () -> {
                String dim = dimId(player.level());
                List<String> result = new ArrayList<>();
                result.add(GuiRes.dimensionName(dim) + " (" + dim + ")");
                result.add(String.format("(%.0f, %.0f, %.0f)", player.getX(), player.getY(), player.getZ()));
                return result;
            });

            Blend blend = new Blend(count * GuiRes.ElemTimeStep, 0.2);
            Widget inputText = ret.getWidget("input_text");
            TextBox textBox = inputText.getComponent(TextBox.class);
            textBox.option.color.setAlpha(0);

            inputText.listen(FrameEvent.class, () ->
                textBox.option.color.setAlpha((int) (blend.alpha() * (inputText.isFocused() ? 0.8 : 0.4) * 255)));

            ret.listen(LeftClickEvent.class, () -> {
                if (!inputText.isFocused()) {
                    textBox.allowEdit = true;
                    textBox.content = "";
                    getGui().gainFocus(inputText);
                }
            });

            Runnable confirmInput = () -> {
                String content = textBox.content;
                if (content.length() > 16) content = content.substring(0, 16);
                LTNetDelegate.send(LTNetDelegate.MSG_ADD, player, content,
                    Future.<List<Location>>create(this::updateList));

                getGui().removeFocus();
                textBox.allowEdit = false;
                textBox.content = "Add...";
            };

            inputText.listen(ConfirmInputEvent.class, confirmInput);

            wrapButton(ret.getWidget("btn_confirm"), count, 0.0, confirmInput);

            return ret;
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    // ---- Data ----

    @NetworkS11nType
    @SerializeStrategy(strategy=ExposeStrategy.ALL)
    public static class Location {
        public String name;
        public String dim;
        public float x;
        public float y;
        public float z;
        public int id;

        public Location() {}

        public Location(String _name, String _dim, float _x, float _y, float _z, int _id) {
            name = _name;
            dim = _dim;
            x = _x;
            y = _y;
            z = _z;
            id = _id;
        }
    }

    @RegDataPart(Player.class)
    public static class LocTeleportData extends DataPart<Player> {

        public static LocTeleportData get(Player player) {
            return EntityData.get(player).getPart(LocTeleportData.class);
        }

        @SerializeIncluded
        public ArrayList<Location> locationList = new ArrayList<>();

        public LocTeleportData() {
            setNBTStorage();
            setClientNeedSync();
        }

        @Override
        public void toNBT(CompoundTag tag) {
            NBTS11n.write(tag, this);
        }

        @Override
        public void fromNBT(CompoundTag tag) {
            NBTS11n.read(tag, this);
        }

        public void add(String name, String dim, float x, float y, float z) {
            checkSide(Side.SERVER);
            locationList.add(new Location(name, dim, x, y, z, locationList.size()));
            sync();
        }

        public void remove(int id) {
            checkSide(Side.SERVER);
            locationList.remove(id);

            for (int i = 0; i < locationList.size(); ++i) locationList.get(i).id = i;

            sync();
        }

        public List<Location> locations() {
            return new ArrayList<>(locationList);
        }
    }

}
