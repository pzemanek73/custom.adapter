package com.phrase.custom.adapter.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record Locale(@JsonValue @NotNull String locale) {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public Locale(String locale) {

        if (!List.of("en", "de", "cs").contains(locale)) {
            throw new IllegalArgumentException("%s is not a valid Phrase locale".formatted(locale));
        }
        this.locale = locale;
    }

    @Override
    public @NotNull String toString() {
        return locale;
    }
}
