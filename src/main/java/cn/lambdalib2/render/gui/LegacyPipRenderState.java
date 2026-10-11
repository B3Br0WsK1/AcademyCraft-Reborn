package cn.lambdalib2.render.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.jspecify.annotations.Nullable;

/**
 * A picture-in-picture GUI element drawn with the legacy GL emulation (3D content inside a GUI rect).
 *  The drawing origin is at the center of the rect, x to the right, y up; one unit is {@code scale} GUI pixels.
 */
@Environment(EnvType.CLIENT)
public record LegacyPipRenderState(int x0, int y0, int x1, int y1, float scale, Runnable draw,
                                   @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds)
        implements PictureInPictureRenderState {

    public LegacyPipRenderState(int x0, int y0, int x1, int y1, float scale, Runnable draw, @Nullable ScreenRectangle scissorArea) {
        this(x0, y0, x1, y1, scale, draw, scissorArea, PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
    }

}
