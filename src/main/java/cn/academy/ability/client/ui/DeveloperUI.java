package cn.academy.ability.client.ui;

import cn.academy.ability.develop.IDeveloper;
import cn.lambdalib2.cgui.CGuiScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class DeveloperUI {
    private DeveloperUI() {}

    public static CGuiScreen apply(IDeveloper tile) {
        return SkillTree.developerUI(tile);
    }
}
