package org.ugoptimizer.gui.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Presentation formatters for canonical system values.
 *
 * <p>Labels map existing canonical dataset values to readable display text.
 * No new domain values are introduced; the formatters only rename what the
 * system already stores.</p>
 */
public final class UiFormatters {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter TIME_ONLY =
            DateTimeFormatter.ofPattern("HH:mm")
                    .withZone(ZoneId.systemDefault());

    private UiFormatters() {
        throw new AssertionError("Utility class must not be instantiated");
    }

    /** Converts an upper-snake canonical value to title case, e.g. THEFT_REPORT -> Theft Report. */
    public static String humanize(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String[] words = value.toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)));
            builder.append(word.substring(1));
        }
        return builder.toString();
    }

    public static String formatInstant(Instant instant) {
        return instant == null ? "-" : DATE_TIME.format(instant);
    }

    public static String formatTimeOnly(Instant instant) {
        return instant == null ? "-" : TIME_ONLY.format(instant);
    }

    /** Urgency is 1..5 with 5 most urgent; the label is presentation only. */
    public static String urgencyLabel(int urgency) {
        return switch (urgency) {
            case 5 -> "Critical";
            case 4 -> "High";
            case 3 -> "Medium";
            case 2 -> "Low";
            default -> "Informational";
        };
    }

    public static String shiftText(String start, String end) {
        if (start == null && end == null) {
            return "No shift";
        }
        if (start == null || end == null) {
            return "Partial shift";
        }
        return start + " - " + end;
    }
}
