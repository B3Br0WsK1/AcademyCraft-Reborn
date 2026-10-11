package cn.lambdalib2.util.markdown;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple markdown dialect parser. Current supported syntax as follows:
 * <ul>
 *   <li>Header: 1~6 `#`s followed by text</li>
 *   <li>List element: `*` followed by a ` ` followed by text</li>
 *   <li>Emphasize: `**` followed by text followed by `**`</li>
 *   <li>Strong: `__` followed by text followed by `__`</li>
 *   <li>Reference: `> ` followed by text</li>
 * </ul>
 * Anything that are not text are treated as <b>tags</b>: inline tag `![tag_name attr1=val1 attr2="val 2" ...]`,
 *  or image tag `![hover](url)`, which is translated to `![img src=url hover=hover]`.
 * Ported from Scala.
 */
public final class MarkdownParser {

    private MarkdownParser() {}

    public interface Attribute {}
    public record ListElement() implements Attribute {}
    public record Header(int level) implements Attribute {}
    public record Emphasize() implements Attribute {}
    public record Strong() implements Attribute {}
    public record Reference() implements Attribute {}

    public interface Instruction {}
    public record Tag(String name, Map<String, String> attrs) implements Instruction {}
    public record Text(String text, Set<Attribute> attrs) implements Instruction {}

    public static void accept(String content, MarkdownRenderer target) {
        content.lines().forEach(ln -> parseLine(ln, target));
    }

    private static void parseLine(String ln, MarkdownRenderer target) {
        Set<Attribute> attributes = new HashSet<>();
        if (ln.startsWith("#")) {
            int sharps = 0;
            while (sharps < ln.length() && ln.charAt(sharps) == '#') ++sharps;
            int level = Math.min(6, sharps);
            attributes.add(new Header(level));
            processSpan(ln.substring(sharps).replaceFirst("^ +", ""), attributes, target);
            target.onNewline();
        } else if (ln.startsWith("* ")) {
            attributes.add(new ListElement());
            processSpan(ln.substring(2), attributes, target);
            target.onNewline();
        } else if (ln.startsWith("> ")) {
            attributes.add(new Reference());
            processSpan(ln.substring(2), attributes, target);
        } else {
            processSpan(ln, attributes, target);
        }
    }

    private static final Pattern star = Pattern.compile("(.*)\\*\\*(.*)", Pattern.DOTALL);
    private static final Pattern underscore = Pattern.compile("(.*)__(.*)", Pattern.DOTALL);
    private static final Pattern image = Pattern.compile("(.*)!\\[([^\\[\\]]*)\\]\\(([^\\(\\)]+)\\)(.*)", Pattern.DOTALL);
    private static final Pattern inlineTag = Pattern.compile("(.*)!\\[([^\\[\\]]+)\\](.*)", Pattern.DOTALL);

    private static void processSpan(String ln, Set<Attribute> attrs, MarkdownRenderer target) {
        List<Instruction> list = parseSpan(ln, attrs);
        if (list.isEmpty()) {
            target.onNewline();
        } else {
            for (Instruction i : list) {
                if (i instanceof Text t) {
                    target.onTextContent(t.text(), t.attrs());
                } else if (i instanceof Tag t) {
                    target.onTag(t.name(), t.attrs());
                }
            }
        }
    }

    private static final Pattern stringProperty = Pattern.compile(" *([^=]+)= *\"([^\"]+)\"(.*)", Pattern.DOTALL);
    private static final Pattern property = Pattern.compile(" *([^=]+)= *([^ =]+)(.*)", Pattern.DOTALL);

    private static Tag parseTag(String content) {
        Map<String, String> props = new HashMap<>();
        int rwind = content.indexOf(' ');
        int ind = rwind == -1 ? content.length() : rwind;
        String rest = content.substring(ind);
        while (true) {
            Matcher m;
            if ((m = stringProperty.matcher(rest)).matches()) {
                props.put(m.group(1).trim(), m.group(2).trim());
                rest = m.group(3);
            } else if ((m = property.matcher(rest)).matches()) {
                props.put(m.group(1).trim(), m.group(2).trim());
                rest = m.group(3);
            } else {
                break;
            }
        }
        return new Tag(content.substring(0, ind), props);
    }

    private static Set<Attribute> with(Set<Attribute> base, Attribute a) {
        Set<Attribute> ret = new HashSet<>(base);
        ret.add(a);
        return ret;
    }

    private static List<Instruction> parseSpan(String line, Set<Attribute> baseattr) {
        List<Instruction> ret = new ArrayList<>();
        Matcher m;
        if ((m = image.matcher(line)).matches()) {
            ret.addAll(parseSpan(m.group(1), baseattr));
            Map<String, String> attrs = new HashMap<>();
            attrs.put("hover", m.group(2));
            attrs.put("src", m.group(3));
            ret.add(new Tag("img", attrs));
            ret.addAll(parseSpan(m.group(4), baseattr));
            return ret;
        }
        if ((m = inlineTag.matcher(line)).matches()) {
            ret.addAll(parseSpan(m.group(1), baseattr));
            ret.add(parseTag(m.group(2)));
            ret.addAll(parseSpan(m.group(3), baseattr));
            return ret;
        }
        Matcher outer = star.matcher(line);
        if (outer.matches()) {
            Matcher inner = star.matcher(outer.group(1));
            if (inner.matches()) {
                ret.addAll(parseSpan(inner.group(1), baseattr));
                ret.add(new Text(inner.group(2), with(baseattr, new Emphasize())));
                ret.addAll(parseSpan(outer.group(2), baseattr));
                return ret;
            }
        }
        outer = underscore.matcher(line);
        if (outer.matches()) {
            Matcher inner = underscore.matcher(outer.group(1));
            if (inner.matches()) {
                ret.addAll(parseSpan(inner.group(1), baseattr));
                ret.add(new Text(inner.group(2), with(baseattr, new Strong())));
                ret.addAll(parseSpan(outer.group(2), baseattr));
                return ret;
            }
        }
        if (!line.isEmpty()) {
            ret.add(new Text(line, baseattr));
        }
        return ret;
    }

}
