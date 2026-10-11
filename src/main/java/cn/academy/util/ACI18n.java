package cn.academy.util;

import net.minecraft.locale.Language;

import java.util.IllegalFormatException;

/**
 * Side-independent string translation. On the client this uses the currently selected language, on a dedicated
 *  server the default (en_us) language, both including the mod's language files.
 * Replacement of 1.12's (deprecated) server-side I18n and of the client I18n.format.
 */
public final class ACI18n {

    private ACI18n() {}

    public static String translate(String key) {
        return Language.getInstance().getOrDefault(key);
    }

    public static String translateFormatted(String key, Object... args) {
        String pattern = translate(key);
        if (args == null || args.length == 0) {
            return pattern;
        }
        try {
            return String.format(pattern, args);
        } catch (IllegalFormatException e) {
            return pattern;
        }
    }

    public static boolean canTranslate(String key) {
        return Language.getInstance().has(key);
    }

    public static String format(String key, Object... args) {
        return translateFormatted(key, args);
    }
}
