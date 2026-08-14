package org.ugoptimizer.gui.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class Messages {

    private static final String BUNDLE_NAME = "org.ugoptimizer.gui.messages";
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME, Locale.getDefault());

    private Messages() {
        throw new AssertionError("Utility class must not be instantiated");
    }

    public static String get(String key) {
        try {
            return BUNDLE.getString(key);
        } catch (MissingResourceException ex) {
            return key;
        }
    }

    public static String format(String key, Object... args) {
        String pattern = get(key);
        return MessageFormat.format(pattern, args);
    }
}
