package org.ugoptimizer.ui;

import java.util.Objects;

/** Presentation label paired with the unchanged domain value passed to backend services. */
public record UiOption<T>(T value, String label) {

    public UiOption {
        Objects.requireNonNull(value, "value cannot be null");
        Objects.requireNonNull(label, "label cannot be null");
        if (label.isBlank()) {
            throw new IllegalArgumentException("label cannot be blank");
        }
    }

    @Override
    public String toString() {
        return label;
    }
}
