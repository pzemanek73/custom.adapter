package com.phrase.custom.adapter.service;

import com.phrase.custom.adapter.dto.Locale;
import com.phrase.custom.adapter.dto.request.TranslateRequest;
import com.phrase.custom.adapter.dto.request.TranslateRequest.Segment;
import com.phrase.custom.adapter.dto.response.TranslateResponse;
import com.phrase.custom.adapter.service.TranslationService.AsyncJobResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class TranslationServiceTest {

    private final TranslationService translationService = new TranslationService();

    @Test
    void translateReturnsTranslatedSegmentsAndPreservesMetadata() {
        TranslateRequest request = sampleRequest();

        TranslateResponse response = translationService.translate(request);

        assertThat(response.sourceLanguage()).isEqualTo(new Locale("en"));
        assertThat(response.targetLanguage()).isEqualTo(new Locale("de"));
        assertThat(response.metadata()).isEqualTo(Map.of("requestId", "sync-1"));
        assertThat(response.segments()).hasSize(2);
        assertThat(response.segments().getFirst().translatedText()).isEqualTo("Hello [de]");
        assertThat(response.segments().get(1).translatedText()).isEqualTo("World [de]");
    }

    @Test
    void translateAsyncCompletesWithTranslatedResponse() throws Exception {
        TranslateRequest request = sampleRequest();

        CompletableFuture<AsyncJobResult> future = translationService.translateAsync(request);
        AsyncJobResult result = future.get(2, TimeUnit.SECONDS);

        assertThat(result.translateResponse()).isNotNull();
        assertThat(result.failureDetail()).isNull();
        assertThat(result.translateResponse().segments().getFirst().translatedText()).isEqualTo("Hello [de]");
        assertThat(result.translateResponse().metadata()).isEqualTo(Map.of("requestId", "sync-1"));
    }

    private TranslateRequest sampleRequest() {
        return new TranslateRequest(
                new Locale("en"),
                new Locale("de"),
                List.of(
                        new Segment("1", "Hello", Map.of("segmentId", "s1")),
                        new Segment("2", "World", Map.of("segmentId", "s2"))
                ),
                null,
                Map.of("requestId", "sync-1")
        );
    }
}
