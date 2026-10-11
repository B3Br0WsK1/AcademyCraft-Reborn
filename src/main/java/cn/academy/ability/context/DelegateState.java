package cn.academy.ability.context;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.SideUtils;
import cn.lambdalib2.util.Side;
import net.fabricmc.api.Environment;
import cn.lambdalib2.util.Color;
import net.fabricmc.api.EnvType;


public enum DelegateState {
    IDLE(0.7f, 0x00000000, false),
    CHARGE(1.0f, 0xffffad37, true),
    ACTIVE(1.0f, 0xff46b3ff, true);

    public final float alpha;
    public final boolean sinEffect;

    @Environment(EnvType.CLIENT)
    public Color glowColor;

    DelegateState(float _alpha, int _glowColor, boolean _sinEffect) {
        alpha = _alpha;
        sinEffect = _sinEffect;
        if (SideUtils.isClient())
            glowColor = Colors.fromHexColor(_glowColor);
    }
}
