package cn.academy.tutorial.client;

import cn.academy.AcademyCraft;
import cn.academy.tutorial.TutorialData;
import cn.academy.util.LocalHelper;
import cn.lambdalib2.input.KeyManager;
import cn.lambdalib2.util.SideUtils;
import cn.lambdalib2.util.markdown.GLMarkdownRenderer;
import cn.lambdalib2.util.markdown.MarkdownParser;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Map;
import java.util.Set;

/**
 * This class renderers markdown with keyid display support. Ported from Scala.
 */
@Environment(EnvType.CLIENT)
public class ACMarkdownRenderer extends GLMarkdownRenderer {

    private final LocalHelper tutLocal = LocalHelper.at("ac.tutorial");

    @Override
    public void onTag(String name, Map<String, String> attr) {
        super.onTag(name, attr);

        if (name.equals("key")) {
            String keyid = attr.get("id");
            var cfg = AcademyCraft.config;
            if (keyid != null && cfg.hasKey("keys", keyid)) {
                onTextContent(KeyManager.getKeyName(cfg.get("keys", keyid, -1).getInt()), Set.of(new MarkdownParser.Reference()));
            } else {
                onTextContent("???", Set.of(new MarkdownParser.Reference()));
            }
        }

        if (name.equals("misakaname")) {
            String n = tutLocal.getFormatted("misaka", TutorialData.get(SideUtils.getThePlayer()).getMisakaID());
            onTextContent(n, Set.of(new MarkdownParser.Strong()));
        }
    }

}
