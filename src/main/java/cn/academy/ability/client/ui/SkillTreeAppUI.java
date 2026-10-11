package cn.academy.ability.client.ui;

import cn.lambdalib2.cgui.CGuiScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class SkillTreeAppUI {
    private SkillTreeAppUI() {}

    public static CGuiScreen apply() {
        return SkillTree.skillTreeAppUI();
    }
}
