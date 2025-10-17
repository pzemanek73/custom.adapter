package com.phrase.custom.adapter.dto.response;


import com.phrase.custom.adapter.dto.Locale;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public record TranslateResponse(@NotNull Locale sourceLanguage, @NotNull Locale targetLanguage,
                                @NotNull List<TranslatedSegment> segments,
                                @Nullable Map<String, Object> metadata) {

    public record TranslatedSegment(@Nullable String idx,
                                    @NotNull String text, @NotNull String translatedText,
                                    @Nullable Map<String, Object> metadata) {
    }

}
