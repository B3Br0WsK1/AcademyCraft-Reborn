package cn.lambdalib2.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * A small re-implementation of Forge's Configuration (the subset used by AcademyCraft), reading and writing the same
 *  ".cfg" text format:
 * <pre>
 * category {
 *     # comment
 *     I:name=5
 *     S:list &lt;
 *         a
 *         b
 *      &gt;
 * }
 * </pre>
 */
public class Configuration {

    public static final String CATEGORY_GENERAL = "general";

    private final File file;
    private final Map<String, ConfigCategory> categories = new TreeMap<>();
    private boolean changed = false;

    public Configuration(File file) {
        this.file = file;
        load();
    }

    public File getConfigFile() {
        return file;
    }

    public synchronized void load() {
        categories.clear();
        if (file == null || !file.isFile()) return;
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            parse(reader);
        } catch (IOException | RuntimeException e) {
            Debug.error("Failed to read config " + file, e);
        }
    }

    private void parse(BufferedReader reader) throws IOException {
        Deque<String> catStack = new ArrayDeque<>();
        String line;
        Property listProp = null;
        List<String> listValues = null;
        while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();
            if (listProp != null) {
                if (trimmed.equals(">")) {
                    listProp.values = listValues.toArray(new String[0]);
                    listProp = null;
                    listValues = null;
                } else if (!trimmed.isEmpty()) {
                    listValues.add(trimmed);
                }
                continue;
            }
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            if (trimmed.endsWith("{")) {
                String name = trimmed.substring(0, trimmed.length() - 1).trim();
                name = unquote(name);
                String full = catStack.isEmpty() ? name : catStack.peek() + "." + name;
                catStack.push(full);
                getCategory(full);
                continue;
            }
            if (trimmed.equals("}")) {
                if (!catStack.isEmpty()) catStack.pop();
                continue;
            }
            if (catStack.isEmpty()) continue;
            ConfigCategory cat = getCategory(catStack.peek());
            int colon = trimmed.indexOf(':');
            if (colon < 1) continue;
            char typeChar = trimmed.charAt(0);
            Property.Type type = Property.Type.fromChar(typeChar);
            String rest = trimmed.substring(colon + 1);
            if (rest.endsWith("<")) {
                String name = unquote(rest.substring(0, rest.length() - 1).trim());
                listProp = new Property(name, new String[0], type);
                listValues = new ArrayList<>();
                cat.put(name, listProp);
            } else {
                int eq = rest.indexOf('=');
                if (eq < 0) continue;
                String name = unquote(rest.substring(0, eq).trim());
                String value = rest.substring(eq + 1);
                cat.put(name, new Property(name, value, type));
            }
        }
    }

    private static String unquote(String s) {
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static String quoteIfNeeded(String s) {
        for (char c : s.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '-' && c != '.') {
                return "\"" + s + "\"";
            }
        }
        return s;
    }

    public synchronized void save() {
        if (file == null) return;
        try {
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            try (Writer w = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                w.write("# Configuration file\n\n");
                for (ConfigCategory cat : categories.values()) {
                    w.write(quoteIfNeeded(cat.name) + " {\n");
                    for (Property p : cat.properties.values()) {
                        if (p.comment != null && !p.comment.isEmpty()) {
                            for (String c : p.comment.split("\n")) {
                                w.write("    # " + c + "\n");
                            }
                        }
                        if (p.isList) {
                            w.write("    " + p.type.ch + ":" + quoteIfNeeded(p.name) + " <\n");
                            for (String v : p.values) {
                                w.write("        " + v + "\n");
                            }
                            w.write("     >\n");
                        } else {
                            w.write("    " + p.type.ch + ":" + quoteIfNeeded(p.name) + "=" + p.value + "\n");
                        }
                    }
                    w.write("}\n\n");
                }
            }
            changed = false;
        } catch (IOException e) {
            Debug.error("Failed to save config " + file, e);
        }
    }

    public boolean hasChanged() {
        return changed;
    }

    public synchronized ConfigCategory getCategory(String category) {
        return categories.computeIfAbsent(category.toLowerCase(Locale.ROOT), ConfigCategory::new);
    }

    public boolean hasCategory(String category) {
        return categories.containsKey(category.toLowerCase(Locale.ROOT));
    }

    public boolean hasKey(String category, String key) {
        ConfigCategory cat = categories.get(category.toLowerCase(Locale.ROOT));
        return cat != null && cat.properties.containsKey(key);
    }

    private Property getOrCreate(String category, String key, String defaultValue, String comment, Property.Type type) {
        ConfigCategory cat = getCategory(category);
        Property prop = cat.properties.get(key);
        if (prop == null || prop.isList) {
            prop = new Property(key, defaultValue, type);
            cat.put(key, prop);
            changed = true;
        } else if (prop.type != type) {
            prop.type = type;
        }
        prop.defaultValue = defaultValue;
        if (comment != null) prop.comment = comment;
        return prop;
    }

    private Property getOrCreateList(String category, String key, String[] defaultValues, String comment, Property.Type type) {
        ConfigCategory cat = getCategory(category);
        Property prop = cat.properties.get(key);
        if (prop == null || !prop.isList) {
            prop = new Property(key, defaultValues, type);
            cat.put(key, prop);
            changed = true;
        }
        prop.defaultValues = defaultValues;
        if (comment != null) prop.comment = comment;
        return prop;
    }

    // --- Property accessors (Forge style)

    public Property get(String category, String key, int defaultValue) {
        return get(category, key, defaultValue, null);
    }

    public Property get(String category, String key, int defaultValue, String comment) {
        return getOrCreate(category, key, String.valueOf(defaultValue), comment, Property.Type.INTEGER);
    }

    public Property get(String category, String key, double defaultValue) {
        return get(category, key, defaultValue, null);
    }

    public Property get(String category, String key, double defaultValue, String comment) {
        return getOrCreate(category, key, String.valueOf(defaultValue), comment, Property.Type.DOUBLE);
    }

    public Property get(String category, String key, boolean defaultValue) {
        return get(category, key, defaultValue, null);
    }

    public Property get(String category, String key, boolean defaultValue, String comment) {
        return getOrCreate(category, key, String.valueOf(defaultValue), comment, Property.Type.BOOLEAN);
    }

    public Property get(String category, String key, String defaultValue) {
        return get(category, key, defaultValue, null);
    }

    public Property get(String category, String key, String defaultValue, String comment) {
        return getOrCreate(category, key, defaultValue, comment, Property.Type.STRING);
    }

    public Property get(String category, String key, String[] defaultValues) {
        return get(category, key, defaultValues, null);
    }

    public Property get(String category, String key, String[] defaultValues, String comment) {
        return getOrCreateList(category, key, defaultValues, comment, Property.Type.STRING);
    }

    public Property get(String category, String key, double[] defaultValues) {
        return get(category, key, defaultValues, null);
    }

    public Property get(String category, String key, double[] defaultValues, String comment) {
        String[] strs = new String[defaultValues.length];
        for (int i = 0; i < strs.length; ++i) strs[i] = String.valueOf(defaultValues[i]);
        return getOrCreateList(category, key, strs, comment, Property.Type.DOUBLE);
    }

    public Property get(String category, String key, int[] defaultValues, String comment) {
        String[] strs = new String[defaultValues.length];
        for (int i = 0; i < strs.length; ++i) strs[i] = String.valueOf(defaultValues[i]);
        return getOrCreateList(category, key, strs, comment, Property.Type.INTEGER);
    }

    // --- Value accessors

    public boolean getBoolean(String name, String category, boolean defaultValue, String comment) {
        return get(category, name, defaultValue, comment).getBoolean(defaultValue);
    }

    public int getInt(String name, String category, int defaultValue, int minValue, int maxValue, String comment) {
        int v = get(category, name, defaultValue, comment).getInt(defaultValue);
        return Math.max(minValue, Math.min(maxValue, v));
    }

    public float getFloat(String name, String category, float defaultValue, float minValue, float maxValue, String comment) {
        double v = get(category, name, (double) defaultValue, comment).getDouble(defaultValue);
        return (float) Math.max(minValue, Math.min(maxValue, v));
    }

    public String getString(String name, String category, String defaultValue, String comment) {
        return get(category, name, defaultValue, comment).getString();
    }

    public String[] getStringList(String name, String category, String[] defaultValues, String comment) {
        return get(category, name, defaultValues, comment).getStringList();
    }

    public static class ConfigCategory {
        public final String name;
        final Map<String, Property> properties = new LinkedHashMap<>();

        ConfigCategory(String name) {
            this.name = name;
        }

        void put(String key, Property p) {
            properties.put(key, p);
        }

        public Property get(String key) {
            return properties.get(key);
        }

        public boolean containsKey(String key) {
            return properties.containsKey(key);
        }

        public Collection<Property> values() {
            return properties.values();
        }

        public Set<Map.Entry<String, Property>> entrySet() {
            return properties.entrySet();
        }

        public String getName() {
            return name;
        }
    }

    public static class Property {

        public enum Type {
            STRING('S'), INTEGER('I'), BOOLEAN('B'), DOUBLE('D');

            final char ch;

            Type(char ch) {
                this.ch = ch;
            }

            static Type fromChar(char c) {
                for (Type t : values()) {
                    if (t.ch == c) return t;
                }
                return STRING;
            }
        }

        private final String name;
        private String value;
        private String[] values;
        private final boolean isList;
        private Type type;
        String comment;
        String defaultValue;
        String[] defaultValues;

        Property(String name, String value, Type type) {
            this.name = name;
            this.value = value;
            this.type = type;
            this.isList = false;
        }

        Property(String name, String[] values, Type type) {
            this.name = name;
            this.values = values;
            this.type = type;
            this.isList = true;
        }

        public String getName() {
            return name;
        }

        public Type getType() {
            return type;
        }

        public String getComment() {
            return comment;
        }

        public boolean isList() {
            return isList;
        }

        public boolean isIntValue() {
            try {
                Integer.parseInt(value);
                return true;
            } catch (RuntimeException e) {
                return false;
            }
        }

        public boolean isDoubleValue() {
            try {
                Double.parseDouble(value);
                return true;
            } catch (RuntimeException e) {
                return false;
            }
        }

        public boolean isBooleanValue() {
            return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
        }

        public String getString() {
            return value;
        }

        public int getInt() {
            return getInt(defaultValue == null ? 0 : parseIntSafe(defaultValue, 0));
        }

        public int getInt(int def) {
            return parseIntSafe(value, def);
        }

        private static int parseIntSafe(String s, int def) {
            try {
                return Integer.parseInt(s.trim());
            } catch (RuntimeException e) {
                return def;
            }
        }

        public double getDouble() {
            return getDouble(0);
        }

        public double getDouble(double def) {
            try {
                return Double.parseDouble(value.trim());
            } catch (RuntimeException e) {
                return def;
            }
        }

        public boolean getBoolean() {
            return getBoolean(false);
        }

        public boolean getBoolean(boolean def) {
            if (isBooleanValue()) return Boolean.parseBoolean(value.trim());
            return def;
        }

        public String[] getStringList() {
            return values == null ? new String[0] : values.clone();
        }

        public double[] getDoubleList() {
            String[] strs = getStringList();
            double[] ret = new double[strs.length];
            for (int i = 0; i < strs.length; ++i) {
                try {
                    ret[i] = Double.parseDouble(strs[i]);
                } catch (RuntimeException ignored) {}
            }
            return ret;
        }

        public int[] getIntList() {
            String[] strs = getStringList();
            int[] ret = new int[strs.length];
            for (int i = 0; i < strs.length; ++i) {
                ret[i] = parseIntSafe(strs[i], 0);
            }
            return ret;
        }

        public void set(String v) {
            value = v;
        }

        public void set(int v) {
            value = String.valueOf(v);
        }

        public void set(double v) {
            value = String.valueOf(v);
        }

        public void set(boolean v) {
            value = String.valueOf(v);
        }

        public void set(String[] v) {
            values = v.clone();
        }

        public void set(double[] v) {
            String[] strs = new String[v.length];
            for (int i = 0; i < v.length; ++i) strs[i] = String.valueOf(v[i]);
            values = strs;
        }

        public void setToDefault() {
            if (isList) {
                values = defaultValues == null ? new String[0] : defaultValues.clone();
            } else {
                value = defaultValue;
            }
        }

        @Override
        public String toString() {
            return isList ? Arrays.toString(values) : value;
        }
    }
}
