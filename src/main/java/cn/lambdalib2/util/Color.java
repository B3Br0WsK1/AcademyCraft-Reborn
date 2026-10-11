package cn.lambdalib2.util;

/**
 * A mutable RGBA color with 0~255 integer channels. Replacement of LWJGL2's org.lwjgl.util.Color, which the original
 *  code relied on.
 */
public class Color {

    private int red, green, blue, alpha;

    public Color() {
        this(0, 0, 0, 255);
    }

    public Color(int r, int g, int b) {
        this(r, g, b, 255);
    }

    public Color(int r, int g, int b, int a) {
        set(r, g, b, a);
    }

    public Color(Color c) {
        setColor(c);
    }

    public void set(int r, int g, int b, int a) {
        red = r & 0xFF;
        green = g & 0xFF;
        blue = b & 0xFF;
        alpha = a & 0xFF;
    }

    public void set(int r, int g, int b) {
        set(r, g, b, 255);
    }

    public void setColor(Color c) {
        set(c.red, c.green, c.blue, c.alpha);
    }

    public int getRed() {
        return red;
    }

    public int getGreen() {
        return green;
    }

    public int getBlue() {
        return blue;
    }

    public int getAlpha() {
        return alpha;
    }

    public void setRed(int red) {
        this.red = red & 0xFF;
    }

    public void setGreen(int green) {
        this.green = green & 0xFF;
    }

    public void setBlue(int blue) {
        this.blue = blue & 0xFF;
    }

    public void setAlpha(int alpha) {
        this.alpha = alpha & 0xFF;
    }

    public byte getRedByte() {
        return (byte) red;
    }

    public byte getGreenByte() {
        return (byte) green;
    }

    public byte getBlueByte() {
        return (byte) blue;
    }

    public byte getAlphaByte() {
        return (byte) alpha;
    }

    public float getRedF() {
        return red / 255f;
    }

    public float getGreenF() {
        return green / 255f;
    }

    public float getBlueF() {
        return blue / 255f;
    }

    public float getAlphaF() {
        return alpha / 255f;
    }

    /**
     * @return The color packed as ARGB (the format used by Minecraft's GUI rendering).
     */
    public int toARGB() {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    public Color copy() {
        return new Color(this);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Color c)) return false;
        return c.red == red && c.green == green && c.blue == blue && c.alpha == alpha;
    }

    @Override
    public int hashCode() {
        return toARGB();
    }

    @Override
    public String toString() {
        return "Color[" + red + ", " + green + ", " + blue + ", " + alpha + "]";
    }
}
