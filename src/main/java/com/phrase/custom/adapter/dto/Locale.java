package com.phrase.custom.adapter.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record Locale(@JsonValue @NotNull String locale) {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public Locale(String locale) {

        // This is just a small scale example, please adhere to the Locale enum values from the OpenAPI spec
        if (!List.of("en", "de", "cs", "zh_tw").contains(locale)) {
            throw new IllegalArgumentException("%s is not a valid Phrase locale code/syntax".formatted(locale));
        }
        this.locale = locale;
    }

    @Override
    public @NotNull String toString() {
        return locale;
    }
}
