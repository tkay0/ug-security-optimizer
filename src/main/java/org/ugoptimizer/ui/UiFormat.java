package org.ugoptimizer.ui;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.ugoptimizer.model.Location;

/** Shared operator-facing formatting that never changes persisted domain values. */
public final class UiFormat {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter
            .ofPattern("dd MMM uuuu, h:mm a", Locale.ENGLISH)
            .withZone(ZoneId.systemDefault());

    private UiFormat() {
    }

    /** Converts an underscore-separated stored code into title-cased display text. */
    public static String humanize(String value) {
        if (value == null || value.isBlank()) {
            return "Not specified";
        }
        String[] words = value.toLowerCase(Locale.ENGLISH).split("_");
        StringBuilder label = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (label.length() > 0) {
                label.append(' ');
            }
            label.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return label.toString();
    }

    public static UiOption<String> codeOption(String value) {
        return new UiOption<>(value, humanize(value));
    }

    public static UiOption<Integer> urgencyOption(int urgency) {
        String meaning = switch (urgency) {
            case 1 -> "Low";
            case 2 -> "Moderate";
            case 3 -> "High";
            case 4 -> "Very High";
            case 5 -> "Critical";
            default -> throw new IllegalArgumentException("urgency must be between 1 and 5");
        };
        return new UiOption<>(urgency, urgency + " - " + meaning);
    }

    @SuppressWarnings("unchecked")
    public static UiOption<Integer>[] locationOptions(List<Location> locations) {
        UiOption<Integer>[] options = (UiOption<Integer>[]) new UiOption<?>[locations.size()];
        for (int index = 0; index < locations.size(); index++) {
            Location location = locations.get(index);
            options[index] = new UiOption<>(location.getLocationId(), location.getName());
        }
        return options;
    }

    public static String locationName(List<Location> locations, int locationId) {
        for (Location location : locations) {
            if (location.getLocationId() == locationId) {
                return location.getName();
            }
        }
        return "Unknown location";
    }

    public static String dateTime(Instant value) {
        return value == null ? "Not recorded" : DATE_TIME.format(value);
    }
}
