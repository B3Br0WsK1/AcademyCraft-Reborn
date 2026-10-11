package cn.lambdalib2.util.markdown;

import java.util.Map;
import java.util.Set;

/**
 * Abstract markdown renderer. Accepts a series of instruction in sequence and behaves afterwards according to
 *  them.
 */
public interface MarkdownRenderer {

    void onTextContent(String text, Set<MarkdownParser.Attribute> attr);

    void onNewline();

    void onTag(String name, Map<String, String> attr);

}
