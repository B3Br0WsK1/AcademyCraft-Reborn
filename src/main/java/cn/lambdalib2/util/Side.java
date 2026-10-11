package cn.lambdalib2.util;

/**
 * Logical side. Replacement of Forge's relauncher Side.
 */
public enum Side {
    CLIENT, SERVER;

    public boolean isClient() {
        return this == CLIENT;
    }

    public boolean isServer() {
        return this == SERVER;
    }
}
