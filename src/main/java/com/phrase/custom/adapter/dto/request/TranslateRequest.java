package com.phrase.custom.adapter.dto.request;


import com.phrase.custom.adapter.dto.Locale;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public record TranslateRequest(@NotNull Locale sourceLanguage, @NotNull Locale targetLanguage,
                               @NotNull List<Segment> segments,
                               @Nullable List<GlossaryEntry> glossary,
                               @Nullable Map<String, Object> metadata) {

    public record Segment(@Nullable String idx, @NotNull String text, @Nullable Map<String, Object> metadata) {
    }

    public record GlossaryEntry(@NotNull String term, @NotNull String translation) {
    }

}
