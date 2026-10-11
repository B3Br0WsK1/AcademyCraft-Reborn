package cn.academy.misc.media;

import cn.academy.Resources;
import cn.academy.client.auxgui.ACHud;
import cn.academy.misc.media.MediaBackend.PlayInfo;
import cn.lambdalib2.cgui.CGuiScreen;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.WidgetContainer;
import cn.lambdalib2.cgui.component.DragBar;
import cn.lambdalib2.cgui.component.DrawTexture;
import cn.lambdalib2.cgui.component.ElementList;
import cn.lambdalib2.cgui.component.ProgressBar;
import cn.lambdalib2.cgui.component.TextBox;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.cgui.event.LeftClickEvent;
import cn.lambdalib2.cgui.event.LostFocusEvent;
import cn.lambdalib2.cgui.loader.CGUIDocument;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.registry.StateEvents;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.GameTimer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.function.Consumer;

/**
 * The media player GUI.
 * @author WeAthFolD
 */
@Environment(EnvType.CLIENT)
public class MediaGui extends CGuiScreen {

    private static WidgetContainer document;

    private static WidgetContainer document() {
        if (document == null) {
            document = CGUIDocument.read(Resources.getGui("media_player"));
        }
        return document;
    }

    private final MediaBackend backend = MediaBackend.get(thePlayer());

    private final Identifier T_PLAY = Resources.getTexture("guis/apps/media_player/play");
    private final Identifier T_PAUSE = Resources.getTexture("guis/apps/media_player/pause");

    private final Widget pageMain = document().getWidget("back").copy();

    private final MediaAcquireData data = MediaAcquireData.get(thePlayer());
    private final List<Media> allInstalled = MediaManager.allMedias().stream().filter(data::isInstalled).toList();

    private double lastTest = GameTimer.getTime();

    public MediaGui() {
        { // Init media elements
            Widget area = pageMain.getWidget("area");
            ElementList list = new ElementList();

            for (Media media : allInstalled) {
                Widget ret = document().getWidget("back/t_one").copy();
                ret.transform.doesDraw = true;

                ret.getWidget("icon").getComponent(DrawTexture.class).texture = media.cover;
                ret.getWidget("title").getComponent(TextBox.class).content = media.name();
                ret.getWidget("desc").getComponent(TextBox.class).content = media.desc();
                ret.getWidget("time").getComponent(TextBox.class).content = media.displayLength();

                ret.listen(LeftClickEvent.class, (w, e) -> {
                    backend.play(media);
                    updatePlayDisplay();
                });

                if (media.external) {
                    wrapEdit(ret.getWidget("btn_edit_name"), ret.getWidget("title"), newName -> {
                        media.propExternalName().set(newName);
                    });
                    wrapEdit(ret.getWidget("btn_edit_desc"), ret.getWidget("desc"), newDesc -> {
                        media.propExternalDesc().set(newDesc);
                    });
                }

                list.addWidget(ret);
            }

            area.addComponent(list);
        }

        // Update every 500ms
        pageMain.listen(FrameEvent.class, (w, e) -> {
            double time = GameTimer.getTime();
            if (time - lastTest > 0.5) {
                lastTest = time;
                updatePlayDisplay();
            }
        });

        // Scroll bar
        pageMain.getWidget("scroll_bar").listen(DragBar.DraggedEvent.class, (w, evt) -> {
            DragBar vdb = w.getComponent(DragBar.class);
            ElementList list = pageMain.getWidget("area").getComponent(ElementList.class);
            list.setProgress((int) (vdb.getProgress() * list.getMaxProgress()));
        });

        // Play or pause button
        pageMain.getWidget("pop").listen(LeftClickEvent.class, (w, e) -> {
            PlayInfo info = backend.currentPlaying();
            if (info != null) {
                if (info.paused()) {
                    backend.continueCurrent();
                } else {
                    backend.pauseCurrent();
                }
            } else if (backend.lastPlayed() != null) {
                backend.play(backend.lastPlayed());
            } else if (!allInstalled.isEmpty()) {
                backend.play(allInstalled.get(0));
            }

            updatePlayDisplay();
        });

        // Stop button
        pageMain.getWidget("stop").listen(LeftClickEvent.class, (w, e) -> {
            backend.stopCurrent();
            updatePlayDisplay();
        });

        // Volume editing
        pageMain.getWidget("volume_bar").getComponent(DragBar.class).setProgress(backend.getVolume());

        pageMain.getWidget("volume_bar").listen(DragBar.DraggedEvent.class, (w, evt) -> {
            DragBar bar = w.getComponent(DragBar.class);
            backend.setVolume(bar.getProgress());
        });

        gui.addWidget("main", pageMain);

        updatePlayDisplay();
    }

    private void wrapEdit(Widget button, Widget box, Consumer<String> callback) {
        button.transform.doesDraw = true;

        DrawTexture dt = new DrawTexture(null).setColor(Colors.monoBlend(.4f, 0));
        box.addComponent(dt);

        TextBox textBox = box.getComponent(TextBox.class);

        boolean[] canEdit = { false };
        button.listen(LeftClickEvent.class, (w, e) -> {
            if (!canEdit[0]) {
                canEdit[0] = true;
                dt.color.setAlpha(Colors.f2i(.2f));
                textBox.allowEdit = true;
                box.gainFocus();
                box.transform.doesListenKey = true;
            }
        });

        Runnable confirmHandler = () -> {
            if (canEdit[0]) {
                canEdit[0] = false;
                textBox.allowEdit = false;
                box.transform.doesListenKey = false;
                dt.color.setAlpha(0);
                callback.accept(textBox.content);
            }
        };

        box.listen(TextBox.ConfirmInputEvent.class, (w, e) -> confirmHandler.run());
        box.listen(LostFocusEvent.class, (w, e) -> confirmHandler.run());
    }

    /**
     * Fetches current playing info and updates it to all GUI elements.
     */
    private void updatePlayDisplay() {
        TextBox playTimeText = pageMain.getWidget("play_time").getComponent(TextBox.class);
        ProgressBar progress = pageMain.getWidget("progress").getComponent(ProgressBar.class);
        TextBox title = pageMain.getWidget("title").getComponent(TextBox.class);
        DrawTexture popTexture = pageMain.getWidget("pop").getComponent(DrawTexture.class);

        PlayInfo info = backend.currentPlaying();
        if (info != null) {
            playTimeText.setContent(info.displayTime());
            progress.progress = info.media().lengthSecs > 0 ? info.time() / info.media().lengthSecs : 0;
            title.setContent(info.media().name());
            popTexture.setTex(info.paused() ? T_PLAY : T_PAUSE);
        } else {
            playTimeText.setContent("00:00");
            progress.progress = 0;
            title.setContent("");
            popTexture.setTex(T_PLAY);
        }
    }

    private static Player thePlayer() {
        return Minecraft.getInstance().player;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @StateEventCallback
    private static void initAux(StateEvents.Init ev) {
        Widget base = CGUIDocument.read(Resources.getGui("media_player_aux")).getWidget("base");

        ACHud.instance.addElement(base, () -> MediaBackend.get().currentPlaying() != null, "media", base.copy());

        ProgressBar progress = base.getWidget("progress").getComponent(ProgressBar.class);
        TextBox title = base.getWidget("title").getComponent(TextBox.class);
        TextBox timetext = base.getWidget("time").getComponent(TextBox.class);

        double[] lastTest = { GameTimer.getTime() };
        base.listen(FrameEvent.class, (w, e) -> {
            double time = GameTimer.getTime();
            if (time - lastTest[0] > 0.5) {
                lastTest[0] = time;

                PlayInfo info = MediaBackend.get().currentPlaying();
                if (info != null) {
                    progress.progress = info.media().lengthSecs > 0 ? info.time() / info.media().lengthSecs : 0;
                    title.setContent(info.media().name());
                    timetext.setContent(info.displayTime());
                } else {
                    progress.progress = 0;
                    title.setContent("");
                    timetext.setContent("");
                }
            }
        });
    }

}
