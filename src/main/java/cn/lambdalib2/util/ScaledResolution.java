package cn.lambdalib2.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

/**
 * The GUI-scaled screen size. Counterpart of 1.12's ScaledResolution.
 */
@Environment(EnvType.CLIENT)
public class ScaledResolution {

    private final int scaledWidth, scaledHeight;
    private final double scaleFactor;

    public ScaledResolution(Minecraft mc) {
        scaledWidth = mc.getWindow().getGuiScaledWidth();
        scaledHeight = mc.getWindow().getGuiScaledHeight();
        scaleFactor = mc.getWindow().getGuiScale();
    }

    public ScaledResolution(int w, int h, double scale) {
        scaledWidth = w;
        scaledHeight = h;
        scaleFactor = scale;
    }

    public int getScaledWidth() {
        return scaledWidth;
    }

    public int getScaledHeight() {
        return scaledHeight;
    }

    public double getScaledWidth_double() {
        return scaledWidth;
    }

    public double getScaledHeight_double() {
        return scaledHeight;
    }

    public int getScaleFactor() {
        return (int) Math.round(scaleFactor);
    }

    public double getScaleFactorD() {
        return scaleFactor;
    }
}
