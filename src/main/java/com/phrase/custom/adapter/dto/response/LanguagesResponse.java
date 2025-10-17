package com.phrase.custom.adapter.dto.response;


import com.phrase.custom.adapter.dto.Locale;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record LanguagesResponse(@NotNull List<LanguagePair> languagePairs) {

    public record LanguagePair(@NotNull Locale sourceLanguage, @NotNull Locale targetLanguage) {
    }

}
