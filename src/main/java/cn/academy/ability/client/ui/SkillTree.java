package cn.academy.ability.client.ui;

import cn.academy.ACItems;
import cn.academy.AcademyCraft;
import cn.academy.Resources;
import cn.academy.ability.AbilityLocalization;
import cn.academy.ability.Category;
import cn.academy.ability.Skill;
import cn.academy.ability.develop.DevelopData;
import cn.academy.ability.develop.DevelopData.DevState;
import cn.academy.ability.develop.DeveloperType;
import cn.academy.ability.develop.IDeveloper;
import cn.academy.ability.develop.LearningHelper;
import cn.academy.ability.develop.action.DevelopActionLevel;
import cn.academy.ability.develop.action.DevelopActionReset;
import cn.academy.ability.develop.action.DevelopActionSkill;
import cn.academy.ability.develop.condition.IDevCondition;
import cn.academy.block.tileentity.TileDeveloper;
import cn.academy.core.client.ui.WirelessPage;
import cn.academy.datapart.AbilityData;
import cn.academy.energy.api.WirelessHelper;
import cn.academy.energy.api.block.IWirelessUser;
import cn.academy.energy.impl.NodeConn;
import cn.academy.util.LocalHelper;
import cn.lambdalib2.cgui.CGui;
import cn.lambdalib2.cgui.CGuiScreen;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.component.Component;
import cn.lambdalib2.cgui.component.DrawTexture;
import cn.lambdalib2.cgui.component.ProgressBar;
import cn.lambdalib2.cgui.component.TextBox;
import cn.lambdalib2.cgui.component.TextBox.ConfirmInputEvent;
import cn.lambdalib2.cgui.component.Tint;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.cgui.event.GuiEvent;
import cn.lambdalib2.cgui.event.KeyEvent;
import cn.lambdalib2.cgui.event.LeftClickEvent;
import cn.lambdalib2.cgui.loader.CGUIDocument;
import cn.lambdalib2.input.KeyHandler;
import cn.lambdalib2.input.KeyManager;
import cn.lambdalib2.input.Keys;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.render.font.IFont;
import cn.lambdalib2.render.font.IFont.FontAlign;
import cn.lambdalib2.render.font.IFont.FontOption;
import cn.lambdalib2.s11n.network.Future;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.NetworkS11n;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.HudUtils;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.RenderUtils;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleUnaryOperator;

import static cn.lambdalib2.render.legacy.GL11.*;
import static cn.lambdalib2.util.MathUtils.clampd;
import static cn.lambdalib2.util.MathUtils.clampf;
import static cn.lambdalib2.util.MathUtils.lerp;

/**
 * Skill tree / developer UI. Ported from SkillTree.scala.
 * The original used GLSL shaders for the radial progress bars and the monochrome (not learned) icons, and depth
 *  buffer tricks for masking; this port draws the progress as a triangle fan and tints the icons instead.
 */
@Environment(EnvType.CLIENT)
public final class SkillTree {

    private SkillTree() {}

    // ---------------- Public entry points ----------------

    public static CGuiScreen developerUI(IDeveloper tile) {
        TreeScreen ret = new TreeScreen() {
            @Override
            public void removed() {
                tile.onGuiClosed();
                super.removed();
            }

            // Close the link page if we are opening that, otherwise delegate down
            @Override
            public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
                if (event.key() == Keys.KEY_ESCAPE) {
                    Widget page = gui.getWidget("link_page");
                    if (page != null) {
                        page.getComponent(Cover.class).end();
                        return true;
                    }
                }
                return super.keyPressed(event);
            }
        };
        CGui gui = ret.getGui();

        Runnable build = () -> {
            gui.clear();
            gui.addWidget("main", initialize(gui, tile));
        };

        gui.listen(RebuildEvent.class, (w, event) -> build.run());

        build.run();

        return ret;
    }

    public static CGuiScreen skillTreeAppUI() {
        CGuiScreen ret = newScreen();
        CGui gui = ret.getGui();

        gui.addWidget(initialize(gui, null));

        return ret;
    }

    public static CGuiScreen skillPosEditorUI() {
        CGuiScreen ret = newScreen();
        CGui gui = ret.getGui();

        Runnable[] build = new Runnable[1];
        build[0] = () -> {
            gui.clear();

            Widget main = initialize(gui, null);

            gui.addWidget(main);

            main.removeWidget("parent_left");

            AbilityData aData = AbilityData.get(player());
            if (aData.hasCategory()) {
                List<Skill> list = aData.getCategory().getSkillList();
                for (int idx = 0; idx < list.size(); ++idx) {
                    Skill skill = list.get(idx);
                    float y = 5 + idx * 12;
                    Widget box0 = new Widget().size(40, 10).pos(20, y)
                        .addComponent(Resources.newTextBox(new FontOption(8)).setContent(skill.getName()));

                    Widget box1 = posBox(gui, skill.guiX, v -> skill.guiX = (float) v).pos(70, y);
                    Widget box2 = posBox(gui, skill.guiY, v -> skill.guiY = (float) v).pos(93, y);

                    gui.addWidget(box0);
                    gui.addWidget(box1);
                    gui.addWidget(box2);
                }
            }
        };

        build[0].run();
        gui.listen(RebuildEvent.class, (w, event) -> build[0].run());

        return ret;
    }

    private static Widget posBox(CGui gui, double init, DoubleConsumer callback) {
        TextBox text = Resources.newTextBox(new FontOption(8)).setContent(String.valueOf(init));
        text.allowEdit();

        Widget ret = new Widget().size(20, 10)
            .addComponent(new DrawTexture().setTex(null).setColor(Colors.fromFloat(.3f, .3f, .3f, .3f)))
            .addComponent(text);
        ret.listen(ConfirmInputEvent.class, () -> {
            try {
                double num = Double.parseDouble(text.content);
                callback.accept(num);
                gui.postEvent(new RebuildEvent());
            } catch (NumberFormatException ignored) {
            }
        });

        return ret;
    }

    @StateEventCallback
    private static void __initPosEditor(StateEvents.Init ev) {
        if (AcademyCraft.DEBUG_MODE) {
            KeyManager.dynamic.addKeyHandler("skill_tree_pos_editor", Keys.KEY_RMENU, new KeyHandler() {
                @Override
                public void onKeyDown() {
                    Minecraft.getInstance().gui.setScreen(skillPosEditorUI());
                }
            });
        }
    }

    // ---------------- Common ----------------

    private static Widget template;

    private static Widget template() {
        if (template == null) {
            template = CGUIDocument.read(Resources.getGui("rework/page_developer")).getWidget("main");
        }
        return template;
    }

    private static final Identifier texAreaBack = Resources.preloadTexture("guis/effect/effect_developer_background");
    private static final Identifier texSkillBack = Resources.preloadMipmapTexture("guis/developer/skill_back");
    private static final Identifier texSkillMask = Resources.preloadMipmapTexture("guis/developer/skill_radial_mask");
    private static final Identifier texSkillOutline = Resources.preloadMipmapTexture("guis/developer/skill_outline");
    private static final Identifier texLine = Resources.preloadMipmapTexture("guis/developer/line");
    private static final Identifier texViewOutline = Resources.preloadMipmapTexture("guis/developer/skill_view_outline");
    private static final Identifier texViewOutlineGlow = Resources.preloadMipmapTexture("guis/developer/skill_view_outline_glow");
    private static final Identifier texButton = Resources.getTexture("guis/developer/button");

    private static final FontOption foSkillTitle = new FontOption(12, FontAlign.CENTER);
    private static final FontOption foSkillDesc = new FontOption(9, FontAlign.CENTER);
    private static final FontOption foSkillProg = new FontOption(8, FontAlign.CENTER, Colors.fromHexColor(0xffa1e1ff));
    private static final FontOption foSkillUnlearned = new FontOption(10, FontAlign.CENTER, Colors.fromHexColor(0xffff5555));
    private static final FontOption foSkillUnlearned2 = new FontOption(10, FontAlign.CENTER, Colors.fromHexColor(0xaaffffff));
    private static final FontOption foSkillReq = new FontOption(9, FontAlign.RIGHT, Colors.fromHexColor(0xaaffffff));
    private static final FontOption foSkillReqDetail = new FontOption(9, FontAlign.LEFT, Colors.fromHexColor(0xeeffffff));
    private static final FontOption foSkillReqDetail2 = new FontOption(9, FontAlign.LEFT, Colors.fromHexColor(0xffee5858));
    private static final FontOption foLevelTitle = new FontOption(12, FontAlign.CENTER);
    private static final FontOption foLevelReq = new FontOption(9, FontAlign.CENTER);

    private static IFont font() { return Resources.font(); }
    private static IFont fontBold() { return Resources.fontBold(); }

    static final LocalHelper local = LocalHelper.at("ac.skill_tree");

    /**
     * This event is posted on the gui to query for widget reload. Each gui instance must by itself respond to it.
     */
    public static class RebuildEvent implements GuiEvent {}

    public static class CloseEvent implements GuiEvent {}

    static Player player() {
        return Minecraft.getInstance().player;
    }

    static Widget initialize(CGui gui, IDeveloper developer) {
        Widget ret = template().copy();

        AbilityData aData = AbilityData.get(player());
        DevelopData devData = DevelopData.get(player());

        Widget area = ret.getWidget("parent_right/area");

        ItemStack held = player().getItemInHand(InteractionHand.MAIN_HAND);
        if (!aData.hasCategory()) {
            initConsole(area, devData, developer);
        } else if (!held.isEmpty() && held.getItem() == ACItems.magnetic_coil) {
            initReset(area, devData, developer);
        } else { // Initialize skill area
            initSkillArea(gui, area, aData, developer);
        }

        { // Initialize left ability panel
            Widget panel = ret.getWidget("parent_left/panel_ability");

            Category cat = aData.getCategoryNullable();
            Identifier icon;
            String name;
            float prog;
            if (cat != null) {
                icon = cat.getDeveloperIcon();
                name = cat.getDisplayName();
                prog = Math.max(0.02f, aData.getLevelProgress());
            } else {
                icon = Resources.getTexture("guis/icons/icon_nocategory");
                name = "N/A";
                prog = 0.0f;
            }

            panel.getWidget("logo_ability").getComponent(DrawTexture.class).setTex(icon);
            panel.getWidget("text_abilityname").getComponent(TextBox.class).setContent(name);
            panel.getWidget("logo_progress").getComponent(ProgressBar.class).progress = prog;
            panel.getWidget("text_level").getComponent(TextBox.class)
                .setContent(AbilityLocalization.instance.levelDesc(aData.getLevel()));

            panel.getWidget("text_exp").getComponent(TextBox.class)
                .setContent("EXP " + (int) (aData.getLevelProgress() * 100) + "%");

            if (developer != null && aData.hasCategory() && LearningHelper.canLevelUp(developer.getDeveloperType(), aData)) {
                Widget btn = panel.getWidget("btn_upgrade");
                btn.transform.doesDraw = true;
                btn.listen(LeftClickEvent.class, () -> gui.addWidget(levelUpArea(gui, aData, developer)));
                panel.removeWidget("text_level");
            }
        }

        { // Initialize machine panel
            Widget panel = ret.getWidget("parent_left/panel_machine");

            Widget wProgPower = panel.getWidget("progress_power");
            ProgressBar progPower = wProgPower.getComponent(ProgressBar.class);

            Widget wProgRate = panel.getWidget("progress_syncrate");
            ProgressBar progRate = wProgRate.getComponent(ProgressBar.class);

            if (developer != null) {
                wProgPower.listen(FrameEvent.class, () ->
                    progPower.progress = developer.getEnergy() / developer.getMaxEnergy());
                progRate.progress = developer.getDeveloperType().syncRate;
                if (developer instanceof TileDeveloper tile) {
                    send(SkillTreeNetDelegate.MSG_GET_NODE, tile, Future.<String>create(result ->
                        panel.getWidget("button_wireless/text_nodename").getComponent(TextBox.class).content =
                            result != null ? result : "N/A"));
                    panel.getWidget("button_wireless").listen(LeftClickEvent.class, () -> {
                        Widget wirelessPage = WirelessPage.userPage(tile).window().centered();
                        Widget cover = blackCover(gui);
                        cover.addWidget(wirelessPage);

                        cover.listen(LeftClickEvent.class, () -> cover.getComponent(Cover.class).end());
                        cover.listen(CloseEvent.class, () -> gui.postEvent(new RebuildEvent()));

                        gui.addWidget("link_page", cover);
                    });
                } else {
                    panel.getWidget("button_wireless").transform.doesDraw = false;
                    panel.getWidget("text_wireless").transform.doesDraw = false;
                }
            } else {
                ret.getWidget("parent_left/ui_left").getComponent(DrawTexture.class)
                    .setTex(Resources.getTexture("guis/ui/ui_developerleft_skilltree"));
                panel.transform.doesDraw = false;
            }
        }

        return ret;
    }

    private static void initSkillArea(CGui gui, Widget area, AbilityData aData, IDeveloper developer) {
        double back_scale = 1.01;
        double back_scale_inv = 1 / back_scale;
        double max_du = back_scale - 1;
        float max_du_skills = 10;

        float[] d = { 0.0f, 0.0f };

        area.listen(FrameEvent.class, () -> {
            CGui g = area.getGui();

            DoubleUnaryOperator scale = x -> (x - 0.5) * back_scale_inv + 0.5;

            d[0] = clampf(0, 1, g.getMouseX() / g.getWidth()) - 0.5f;
            d[1] = clampf(0, 1, g.getMouseY() / g.getHeight()) - 0.5f;

            // Draw background
            RenderUtils.loadTexture(texAreaBack);
            HudUtils.rawRect(0, 0, scale.applyAsDouble(d[0] * max_du), scale.applyAsDouble(d[1] * max_du),
                area.transform.width, area.transform.height,
                back_scale_inv, back_scale_inv);
        });

        List<Skill> skills = new ArrayList<>();
        for (Skill skill : aData.getCategory().getSkillList()) {
            if (LearningHelper.canBePotentiallyLearned(aData, skill) && skill.isEnabled()) skills.add(skill);
        }

        for (int idx = 0; idx < skills.size(); ++idx) {
            Skill skill = skills.get(idx);
            final int StateIdle = 0;
            final int StateHover = 1;
            final double TransitTime = 0.1;

            final float WidgetSize = 16.0f;
            final float ProgSize = 31.0f;
            final float TotalSize = 23.0f;
            final float IconSize = 14.0f;
            final float ProgAlign = (TotalSize - ProgSize) / 2;
            final float Align = (TotalSize - IconSize) / 2;
            final float DrawAlign = (WidgetSize - TotalSize) / 2;

            boolean learned = aData.isSkillLearned(skill);

            Widget widget = new Widget();
            float sx = skill.guiX, sy = skill.guiY;

            double[] lastTransit = { GameTimer.getTime() - 2 };
            int[] state = { StateIdle };
            double creationTime = GameTimer.getTime();
            double blendOffset = idx * 0.08 + 0.1;

            double mAlpha;
            boolean parentLearned = skill.getParent() == null || aData.isSkillLearned(skill.getParent());
            if (learned) mAlpha = 1.0;
            else if (parentLearned) mAlpha = 0.7;
            else mAlpha = 0.25;

            DoubleConsumer lineDrawer = null;
            if (skill.getParent() != null) {
                Skill parent = skill.getParent();
                double cx = skill.guiX + WidgetSize / 2, cy = skill.guiY + WidgetSize / 2;
                double pcx = parent.guiX + WidgetSize / 2, pcy = parent.guiY + WidgetSize / 2;
                double px = pcx - cx, py = pcy - cy;
                double norm = Math.sqrt(px * px + py * py);
                double ldx = px / norm * 12.2, ldy = py / norm * 12.2;

                lineDrawer = drawLine(px + WidgetSize / 2 - ldx, py + WidgetSize / 2 - ldy,
                    WidgetSize / 2 + ldx, WidgetSize / 2 + ldy, 5.5, mAlpha * (learned ? 1.0 : 0.4));
            }
            DoubleConsumer fLineDrawer = lineDrawer;

            widget.pos(sx, sy).size(WidgetSize, WidgetSize);
            widget.listen(FrameEvent.class, (w, evt) -> {
                double time = GameTimer.getTime();

                widget.pos(sx - d[0] * max_du_skills, sy - d[1] * max_du_skills);
                widget.dirty = true;

                double transitProgress = clampd(0, 1, (time - lastTransit[0]) / TransitTime);
                double scale = state[0] == StateIdle
                    ? lerp(1.2, 1, transitProgress)
                    : lerp(1, 1.2, transitProgress);

                // Transit state
                if (transitProgress == 1) {
                    if (state[0] == StateIdle && evt.hovering) {
                        state[0] = StateHover;
                        lastTransit[0] = GameTimer.getTime();
                    } else if (state[0] == StateHover && !evt.hovering) {
                        state[0] = StateIdle;
                        lastTransit[0] = GameTimer.getTime();
                    }
                }

                double dt = Math.max(0, time - creationTime - blendOffset);
                double backAlpha = mAlpha * clampd(0, 1, dt * 10.0);
                double iconAlpha = mAlpha * clampd(0, 1, (dt - 0.08) * 10.0);
                float progressBlend = (float) clampd(0, 1, (dt - 0.12) * 2.0);
                double lineBlend = clampd(0, 1, dt * 5.0);

                // Connection line (drawn below the node)
                if (fLineDrawer != null) {
                    glPushMatrix();
                    fLineDrawer.accept(lineBlend);
                    glPopMatrix();
                }

                glPushMatrix();

                glTranslated(DrawAlign, DrawAlign, 0);

                glTranslated(TotalSize / 2, TotalSize / 2, 0);
                glScaled(scale, scale, 1);
                glTranslated(-TotalSize / 2, -TotalSize / 2, 0);

                // Draw back
                glColor4d(1, 1, 1, backAlpha);
                RenderUtils.loadTexture(texSkillBack);
                HudUtils.rect(0, 0, TotalSize, TotalSize);

                // Draw outline back
                RenderUtils.loadTexture(texSkillOutline);
                glColor4d(0.2, 0.2, 0.2, backAlpha * 0.6);
                HudUtils.rect(ProgAlign, ProgAlign, ProgSize, ProgSize);

                // Draw skill (monochrome-ish when not learned)
                if (learned) {
                    glColor4d(1, 1, 1, iconAlpha);
                } else {
                    glColor4d(0.55, 0.55, 0.55, iconAlpha);
                }
                RenderUtils.loadTexture(skill.getHintIcon());
                HudUtils.rect(Align, Align, IconSize, IconSize);

                // Progress bar (if learned)
                if (learned) {
                    glColor4d(1, 1, 1, 1);
                    drawRadialProgress(texSkillOutline, ProgAlign, ProgAlign, ProgSize,
                        progressBlend * aData.getSkillExp(skill));
                }

                glColor4f(1, 1, 1, 1);
                glPopMatrix();
            });

            widget.listen(LeftClickEvent.class, () -> widget.getGui().addWidget(skillViewArea(gui, skill, aData, developer)));

            area.addWidget(widget);
        }
    }

    /**
     * Draws the given texture (in the square (x, y, size)) as a radial "pie" from 12 o'clock clockwise up to
     *  the given progress. Replacement of the skill_progbar shader.
     */
    private static void drawRadialProgress(Identifier tex, double x, double y, double size, double progress) {
        progress = clampd(0, 1, progress);
        if (progress <= 0) return;
        RenderUtils.loadTexture(tex);

        double cx = x + size / 2, cy = y + size / 2, r = size / 2;
        int segs = Math.max(1, (int) Math.ceil(progress * 48));
        double total = progress * Math.PI * 2;

        glBegin(GL_TRIANGLES);
        for (int i = 0; i < segs; ++i) {
            double a0 = total * i / segs, a1 = total * (i + 1) / segs;
            // Use a radius that reaches the square's corners so the full texture is covered
            double rr = r * Math.sqrt(2);
            double x0 = Math.sin(a0) * rr, y0 = -Math.cos(a0) * rr;
            double x1 = Math.sin(a1) * rr, y1 = -Math.cos(a1) * rr;
            x0 = clampd(-r, r, x0); y0 = clampd(-r, r, y0);
            x1 = clampd(-r, r, x1); y1 = clampd(-r, r, y1);

            glTexCoord2d(0.5, 0.5);
            glVertex2d(cx, cy);
            glTexCoord2d(0.5 + x0 / size, 0.5 + y0 / size);
            glVertex2d(cx + x0, cy + y0);
            glTexCoord2d(0.5 + x1 / size, 0.5 + y1 / size);
            glVertex2d(cx + x1, cy + y1);
        }
        glEnd();
    }

    private static DoubleConsumer drawLine(double x0, double y0, double x1, double y1, double width, double alpha) {
        double dx = x1 - x0, dy = y1 - y0;
        double norm = Math.sqrt(dx * dx + dy * dy);
        double nx = -dy / norm / 2 * width, ny = dx / norm / 2 * width;

        return progress -> {
            double xx = lerp(x0, x1, progress), yy = lerp(y0, y1, progress);

            RenderUtils.loadTexture(texLine);
            glColor4d(1, 1, 1, alpha);

            glBegin(GL_QUADS);

            glTexCoord2d(0, 0);
            glVertex2d(x0 - nx, y0 - ny);

            glTexCoord2d(0, 1);
            glVertex2d(x0 + nx, y0 + ny);

            glTexCoord2d(1, 1);
            glVertex2d(xx + nx, yy + ny);

            glTexCoord2d(1, 0);
            glVertex2d(xx - nx, yy - ny);

            glEnd();
            glColor4f(1, 1, 1, 1);
        };
    }

    private static Widget blackCover(CGui gui) {
        Widget ret = new Widget();
        ret.addComponent(new Cover());
        ret.size(gui.getWidth(), gui.getHeight());

        return ret;
    }

    private static Widget levelUpArea(CGui gui, AbilityData data, IDeveloper developer) {
        Widget ret = blackCover(gui);

        Widget wid = new Widget();
        wid.centered().size(50, 50);

        DevelopActionLevel action = new DevelopActionLevel();
        double estmCons = LearningHelper.getEstimatedConsumption(player(), developer.getDeveloperType(), action);

        Widget textArea = new Widget().size(0, 10).centered().pos(0, 25);

        String[] hint = { local.get("level_question") };
        double[] progress = { 0 };
        boolean[] canClose = { true };
        boolean[] shouldRebuild = { false };

        Identifier icon = Resources.getTexture("abilities/condition/any" + (data.getLevel() + 1));

        wid.listen(FrameEvent.class, () -> drawActionIcon(icon, progress[0], progress[0] == 1));

        String lvltext = local.getFormatted("uplevel", AbilityLocalization.instance.levelDesc(data.getLevel() + 1));
        String reqtext = local.get("req") + String.format(" %.0f", estmCons);
        textArea.listen(FrameEvent.class, () -> {
            font().draw(lvltext, 0, 3, foLevelTitle);
            font().draw(reqtext, 0, 16, foLevelReq);
            font().draw(hint[0], 0, 26, foLevelReq);
        });

        Widget button = newButton().centered().pos(0, 40);
        button.listen(LeftClickEvent.class, () -> {
            if (developer.getEnergy() < estmCons) {
                hint[0] = local.get("noenergy");
            } else {
                DevelopData devData = DevelopData.get(player());
                devData.reset();
                canClose[0] = false;

                send(SkillTreeNetDelegate.MSG_START_LEVEL, devData, developer);
                ret.listen(FrameEvent.class, () -> {
                    switch (devData.getState()) {
                        case IDLE -> {}
                        case DEVELOPING -> {
                            hint[0] = local.get("dev_developing");
                            progress[0] = devData.getDevelopProgress();
                        }
                        case DONE -> {
                            hint[0] = local.get("dev_successful");
                            progress[0] = 1;
                            canClose[0] = true;
                            shouldRebuild[0] = true;
                        }
                        case FAILED -> {
                            hint[0] = local.get("dev_failed");
                            canClose[0] = true;
                        }
                    }
                });
            }

            button.dispose();
        });

        textArea.addWidget(button);
        ret.addWidget(textArea);
        ret.listen(LeftClickEvent.class, () -> {
            if (canClose[0]) {
                if (shouldRebuild[0]) {
                    gui.postEvent(new RebuildEvent());
                } else {
                    ret.getComponent(Cover.class).end();
                }
            }
        });
        ret.addWidget(wid);

        return ret;
    }

    private static class CondTag extends Component {
        final IDevCondition cond;
        final boolean accepted;

        CondTag(IDevCondition cond, boolean accepted) {
            super("CondTag");
            this.cond = cond;
            this.accepted = accepted;
        }
    }

    private static Widget skillViewArea(CGui gui, Skill skill, AbilityData data, IDeveloper developer) {
        Widget ret = blackCover(gui);

        Widget skillWid = new Widget();
        skillWid.centered().size(50, 50);

        boolean learned = data.isSkillLearned(skill);
        boolean[] canClose = { true };
        boolean[] shouldRebuild = { false };

        Widget textArea = new Widget().size(0, 10).centered().pos(0, 25);
        if (learned) {
            skillWid.listen(FrameEvent.class, () -> drawActionIcon(skill.getHintIcon(), 0, false));
            textArea.listen(FrameEvent.class, () -> {
                fontBold().draw(skill.getDisplayName(), 0, 3, foSkillTitle);
                font().draw(local.get("skill_exp") + (int) (data.getSkillExp(skill) * 100) + "%", 0, 15, foSkillProg);
                font().drawSeperated(skill.getDescription(), 0, 24, 200, foSkillDesc);
            });
        } else {
            double[] progress = { 0 };
            String[] message = { null };

            skillWid.listen(FrameEvent.class, () -> drawActionIcon(skill.getHintIcon(), progress[0], progress[0] == 1));

            String skillNameText = skill.getDisplayName() + " (LV " + skill.getLevel() + ")";
            textArea.listen(FrameEvent.class, () -> {
                fontBold().draw(skillNameText, 0, 3, foSkillTitle);
                font().draw(local.get("skill_not_learned"), 0, 15, foSkillUnlearned);
            });

            if (developer != null) {
                DevelopActionSkill action = new DevelopActionSkill(skill);
                double estmCons = LearningHelper.getEstimatedConsumption(player(), developer.getDeveloperType(), action);

                List<IDevCondition> conditions = new ArrayList<>();
                for (IDevCondition c : skill.getDevConditions()) {
                    if (c.shouldDisplay()) conditions.add(c);
                }
                final int CondIconSize = 14;
                final int CondIconStep = 16;
                int len = CondIconStep * conditions.size();

                textArea.listen(FrameEvent.class, () -> font().draw(local.get("req"), -len / 2f - 2, 28, foSkillReq));

                for (int idx = 0; idx < conditions.size(); ++idx) {
                    IDevCondition cond = conditions.get(idx);
                    Widget widget = new Widget().size(CondIconSize, CondIconSize)
                        .pos(-len / 2f + CondIconStep * idx, 25);

                    DrawTexture tex = new DrawTexture(cond.getIcon());
                    boolean accepted = cond.accepts(data, developer, skill);

                    if (!accepted) {
                        tex.setColor(Colors.fromFloat(0.5f, 0.5f, 0.5f, 1f));
                    }

                    widget.addComponent(tex);
                    widget.addComponent(new CondTag(cond, accepted));
                    textArea.addWidget(widget);
                }

                textArea.listen(FrameEvent.class, () -> {
                    Widget w = gui.getHoveringWidget();
                    if (w != null) {
                        CondTag tag = w.getComponent(CondTag.class);
                        if (tag != null) {
                            font().draw("(" + tag.cond.getHintText() + ")", len / 2f + 3, 27,
                                tag.accepted ? foSkillReqDetail : foSkillReqDetail2);
                        }
                    }
                });

                textArea.listen(FrameEvent.class, () -> {
                    if (message[0] != null) {
                        font().draw(message[0], 0, 40, foSkillUnlearned2);
                    } else {
                        font().draw(String.format(local.get("learn_question"), String.format("%.0f", estmCons)),
                            0, 40, foSkillUnlearned2);
                    }
                });

                Widget button = newButton().centered().pos(0, 55);

                button.listen(LeftClickEvent.class, () -> {
                    if (developer.getEnergy() < estmCons) {
                        message[0] = local.get("noenergy");
                    } else if (skill.getLevel() > data.getLevel()) {
                        message[0] = local.getFormatted("level_fail", skill.getLevel());
                    } else if (!action.validate(player(), developer)) {
                        message[0] = local.get("condition_fail");
                    } else {
                        // start developing
                        DevelopData devData = DevelopData.get(player());
                        devData.reset();

                        send(SkillTreeNetDelegate.MSG_START_SKILL, devData, developer, skill);
                        canClose[0] = false;
                        ret.listen(FrameEvent.class, () -> {
                            switch (devData.getState()) {
                                case IDLE -> {}
                                case DEVELOPING -> {
                                    message[0] = local.get("progress") +
                                        String.format(" %.0f%%", devData.getDevelopProgress() * 100);
                                    progress[0] = devData.getDevelopProgress();
                                }
                                case DONE -> {
                                    message[0] = local.get("dev_successful");
                                    shouldRebuild[0] = true;
                                    progress[0] = 1.0;
                                    canClose[0] = true;
                                }
                                case FAILED -> {
                                    canClose[0] = true;
                                    message[0] = local.get("dev_failed");
                                }
                            }
                        });
                    }

                    button.dispose();
                });

                textArea.addWidget(button);
            }
        }

        ret.addWidget(textArea);
        ret.addWidget(skillWid);

        ret.listen(LeftClickEvent.class, () -> {
            if (canClose[0]) {
                if (shouldRebuild[0]) {
                    gui.postEvent(new RebuildEvent());
                } else {
                    ret.getComponent(Cover.class).end();
                }
            }
        });

        return ret;
    }

    private static Widget newButton() {
        return new Widget()
            .size(64, 32).scale(.5f)
            .addComponent(new DrawTexture(texButton))
            .addComponent(new Tint(Colors.monoBlend(1, .6f), Colors.monoBlend(1, 1), true));
    }

    private static void drawActionIcon(Identifier icon, double progress, boolean glow) {
        final int BackSize = 50;
        final int IconSize = 27;
        final int IconAlign = (BackSize - IconSize) / 2;

        glPushMatrix();
        glColor4f(1, 1, 1, 1);

        RenderUtils.loadTexture(texSkillBack);
        HudUtils.rect(0, 0, BackSize, BackSize);

        RenderUtils.loadTexture(icon);
        HudUtils.rect(IconAlign, IconAlign, IconSize, IconSize);

        drawRadialProgress(glow ? texViewOutlineGlow : texViewOutline, 0, 0, BackSize, progress);

        glPopMatrix();
    }

    private static String fmt(int x) {
        return x < 10 ? "0" + x : String.valueOf(x);
    }

    private static void initConsole(Widget area, DevelopData data, IDeveloper developer) {
        Console console = new Console(false, developer != null);

        if (developer != null) {
            console.addCommand(new Command("learn", () -> {
                console.enqueue(printTask(console, Console.localized("dev_begin")));
                console.enqueue(printTask(console, Console.localized("progress", fmt(0))));
                send(SkillTreeNetDelegate.MSG_START_LEVEL, data, developer);
                data.reset();

                console.enqueue(progressTask(console, data, "dev_succ", "dev_fail"));
            }));
        }

        area.addComponent(console);
    }

    private static void initReset(Widget area, DevelopData data, IDeveloper developer) {
        Console console = new Console(true, true);

        console.addCommand(new Command("reset", () -> {
            if (developer != null && DevelopActionReset.canReset(data.getEntity(), developer)) {
                console.enqueue(printTask(console, Console.localized("reset_begin")));
                console.enqueue(printTask(console, Console.localized("progress", fmt(0))));
                send(SkillTreeNetDelegate.MSG_RESET, data, developer);
                data.reset();

                console.enqueue(progressTask(console, data, "reset_succ", "reset_fail"));
            } else {
                if (developer == null || developer.getDeveloperType() != DeveloperType.ADVANCED) {
                    console.enqueue(printTask(console, Console.localized("reset_fail_dev")));
                } else {
                    console.enqueue(printTask(console, Console.localized("reset_fail_other")));
                }
            }
        }));

        area.addComponent(console);
    }

    private static Task progressTask(Console console, DevelopData data, String succ, String fail) {
        return new Task() {
            @Override
            public boolean isFinished() {
                return data.getState() == DevState.FAILED || data.getState() == DevState.DONE;
            }

            @Override
            public void update() {
                console.output("\b\b\b" + fmt((int) (data.getDevelopProgress() * 100)) + "%");
            }

            @Override
            public void finish() {
                console.outputln();
                if (data.getState() == DevState.DONE) {
                    console.output(Console.localized(succ));
                } else {
                    console.output(Console.localized(fail));
                }

                console.pause(0.5);
                console.enqueueRebuild();
            }
        };
    }

    public static class Cover extends Component {

        private double lastTransit = GameTimer.getTime();
        private boolean ended = false;

        public Cover() {
            super("cover");
            listen(FrameEvent.class, (w, evt) -> {
                double time = GameTimer.getTime();
                double dt = time - lastTransit;

                widget.transform.width = widget.getGui().getWidth();
                widget.transform.height = widget.getGui().getHeight();

                double src = clampd(0, 1, dt / 0.2);
                double alpha = ended ? 1 - src : src;

                glColor4d(0, 0, 0, alpha * 0.7);
                HudUtils.colorRect(0, 0, widget.transform.width, widget.transform.height);
                glColor4f(1, 1, 1, 1);

                if (ended && alpha == 0) {
                    widget.post(new CloseEvent(), false);
                    widget.dispose();
                }

                widget.dirty = true;
            });
        }

        public void end() {
            ended = true;
            lastTransit = GameTimer.getTime();
        }

    }

    static CGuiScreen newScreen() {
        return new TreeScreen();
    }

    public static class TreeScreen extends CGuiScreen {
        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    // ---------------- Console ----------------

    record Command(String name, Runnable callback) {}

    interface Task {
        default void begin() {}
        default void update() {}
        default void finish() {}
        boolean isFinished();
    }

    abstract static class TimedTask implements Task {
        private double creationTime = -1;

        abstract double life();

        double getCreationTime() {
            return creationTime;
        }

        @Override
        public void begin() {
            creationTime = GameTimer.getTime();
        }

        @Override
        public boolean isFinished() {
            return (GameTimer.getTime() - creationTime) >= life();
        }
    }

    static Task printTask(Console console, String str) {
        return new Task() {
            @Override
            public void begin() {
                console.output(str);
            }

            @Override
            public boolean isFinished() {
                return true;
            }
        };
    }

    static Task slowPrintTask(Console console, String str) {
        return new Task() {
            static final double PerCharTime = 0.01;

            private int idx = 0;
            private double last = -1;

            @Override
            public void begin() {
                last = GameTimer.getTime();
            }

            @Override
            public void update() {
                double time = GameTimer.getTime();
                int n = (int) ((time - last) / PerCharTime);
                if (n > 0) {
                    int end = Math.min(str.length(), idx + n);
                    console.output(str.substring(idx, end));

                    last += n * PerCharTime;
                    idx = end;
                }
            }

            @Override
            public boolean isFinished() {
                return idx == str.length();
            }
        };
    }

    static class Console extends Component {

        private static final int MaxLines = 10;
        private static final FontOption FO = new FontOption(8);

        private static final String ConsoleHead = "OS >";

        private static final LocalHelper consoleLocal = local.subPath("console");

        static String localized(String id, Object... args) {
            return consoleLocal.getFormatted(id, args).replace("\\n", "\n");
        }

        final boolean emergency;
        final boolean hasDeveloper;

        private final Task inputTask = new Task() {
            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public void begin() {
                output(ConsoleHead);
            }
        };

        private final List<Command> commands = new ArrayList<>();
        private final Deque<String> outputs = new LinkedList<>();
        private final Queue<Task> taskQueue = new LinkedList<>();
        private Task currentTask = null;
        private String input = "";

        Console(boolean emergency, boolean hasDeveloper) {
            super("Console");
            this.emergency = emergency;
            this.hasDeveloper = hasDeveloper;

            enqueue(slowPrintTask(this, localized("init", player().getName().getString())));
            pause(0.4);

            List<String> numSeq = new ArrayList<>();
            for (int i = 1; i <= 6; ++i) numSeq.add((i * 10 + RandUtils.nextInt(6) - 3) + "%");
            numSeq.add((64 + RandUtils.nextInt(4)) + "%");
            numSeq.add(localized("boot_failed"));

            animSequence(0.3, numSeq);

            String startupText;
            if (emergency) startupText = localized("override");
            else if (hasDeveloper) startupText = localized("invalid_cat") + localized("learn_hint");
            else startupText = localized("invalid_cat");
            enqueue(slowPrintTask(this, startupText));

            listen(FrameEvent.class, (w, evt) -> {
                if (currentTask != null && currentTask.isFinished()) {
                    currentTask.finish();
                    currentTask = null;
                }

                if (currentTask != null) {
                    currentTask.update();
                }

                if (currentTask == null) {
                    if (taskQueue.isEmpty()) {
                        currentTask = inputTask;
                    } else {
                        currentTask = taskQueue.remove();
                    }
                    currentTask.begin();
                }

                float x = 5;
                IFont font = Resources.font();
                float y = 5;

                int idx = 0;
                for (String line : outputs) {
                    if (idx == outputs.size() - 1 && currentTask == inputTask) {
                        boolean cursor = ((int) (GameTimer.getTime() * 1000) % 1000) < 500;
                        font.draw(line + input + (cursor ? "_" : ""), x, y, FO);
                    } else {
                        font.draw(line, x, y, FO);
                    }

                    y += 10;
                    ++idx;
                }

                if (widget.getGui().getWidget("link_page") == null) {
                    widget.gainFocus();
                }
            });

            listen(KeyEvent.class, (w, evt) -> {
                if (evt.keyCode == Keys.KEY_BACK) {
                    if (input.length() > 0) {
                        input = input.substring(0, input.length() - 1);
                    }
                } else if (evt.keyCode == Keys.KEY_RETURN || evt.keyCode == Keys.KEY_NUMPADENTER) {
                    parseCommand(input);
                    input = "";
                } else if (evt.inputChar != '\0' && StringUtil.isAllowedChatCharacter(evt.inputChar)) {
                    input += evt.inputChar;
                }
            });
        }

        void enqueue(Task task) {
            taskQueue.offer(task);
            if (currentTask == inputTask) {
                outputln(input);
                currentTask = null;
            }
        }

        void output(String content) {
            StringBuilder current = new StringBuilder(outputs.isEmpty() ? "" : outputs.removeLast());

            for (char ch : content.toCharArray()) {
                if (ch == '\b') {
                    current.setLength(Math.max(0, current.length() - 1));
                } else if (ch == '\n') {
                    outputs.addLast(current.toString());
                    current = new StringBuilder();
                } else {
                    current.append(ch);
                }
            }

            outputs.addLast(current.toString());

            while (outputs.size() > MaxLines) outputs.removeFirst();
        }

        void outputln(String content) {
            output(content + '\n');
        }

        void outputln() {
            output("\n");
        }

        void animSequence(double time, List<String> strs) {
            for (int idx = 0; idx < strs.size(); ++idx) {
                String s = strs.get(idx);
                boolean last = idx == strs.size() - 1;
                enqueue(new TimedTask() {
                    @Override
                    double life() {
                        return time;
                    }

                    @Override
                    public void finish() {
                        if (!last) {
                            output("\b".repeat(s.length()));
                        }
                    }

                    @Override
                    public void begin() {
                        super.begin();
                        output(s);
                    }
                });
            }
        }

        void pause(double time) {
            enqueue(new TimedTask() {
                @Override
                double life() {
                    return time;
                }
            });
        }

        void enqueueRebuild() {
            enqueue(new Task() {
                @Override
                public boolean isFinished() {
                    return true;
                }

                @Override
                public void begin() {
                    widget.getGui().postEvent(new RebuildEvent());
                }
            });
        }

        void addCommand(Command command) {
            commands.add(command);
        }

        private void parseCommand(String cmd) {
            for (Command c : commands) {
                if (c.name().equals(cmd)) {
                    c.callback().run();
                    return;
                }
            }
            enqueue(printTask(this, localized("invalid_command")));
        }

    }

    private static void send(String channel, Object... pars) {
        NetworkMessage.sendToServer(SkillTreeNetDelegate.INSTANCE, channel, pars);
    }

}
