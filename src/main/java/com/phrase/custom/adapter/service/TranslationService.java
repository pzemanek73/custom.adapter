package com.phrase.custom.adapter.service;

import com.deepl.api.DeepLClient;
import com.deepl.api.TextResult;
import com.phrase.custom.adapter.dto.request.TranslateRequest;
import com.phrase.custom.adapter.dto.response.TranslateResponse;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.lang.Thread.sleep;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Service
public class TranslationService {

    private final Logger logger = LoggerFactory.getLogger(TranslationService.class);

    public @NotNull TranslateResponse translate(TranslateRequest translateRequest) {
        try {
            // Call your engine here

            // Simulating work
            logger.info("Translating...");
            sleep(1000);
        } catch (Exception e) {
            throw new IllegalStateException("translation failed: %s".formatted(e));
        }

        return getTranslateResponse(translateRequest);
    }

    @Async("threadPoolTaskExecutor")
    public @NotNull CompletableFuture<AsyncJobResult> translateAsync(TranslateRequest translateRequest) {
        return CompletableFuture.supplyAsync(() -> {
            TranslateResponse translateResponse = null;
            String failureDetail = null;

            try {
                // Call your engine here

                // Simulating work
                logger.info("Translating...");
                sleep(1000);
                translateResponse = getTranslateResponse(translateRequest);
            } catch (Exception e) {
                failureDetail = "translation failed: %s".formatted(e);
            }

            return new AsyncJobResult(translateResponse, failureDetail);
        });
    }

    private @NotNull TranslateResponse getTranslateResponse(TranslateRequest translateRequest) {
        // Translation simulation loopback (it only adds the target locale to the input segments)

        String authKey = isNull(translateRequest.metadata()) ? null : translateRequest.metadata().getOrDefault("deepl_api_key", "").toString();
        DeepLClient deepLClient = isBlank(authKey) ? null : new DeepLClient(authKey);

        List<TranslateResponse.TranslatedSegment> translatedSegments = isNull(deepLClient)
                ? translateRequest.segments().stream()
                .map(s ->
                        new TranslateResponse.TranslatedSegment(
                                s.idx(),
                                s.text(),
                                "%s [%s]".formatted(s.text(), translateRequest.targetLanguage().locale()),
                                s.metadata()
                        )
                ).toList()
                : getDeepLTranslations(deepLClient, translateRequest.segments(), translateRequest.targetLanguage().locale());

        return new TranslateResponse(
                translateRequest.sourceLanguage(),
                translateRequest.targetLanguage(),
                translatedSegments,
                translateRequest.metadata()
        );
    }

    private List<TranslateResponse.TranslatedSegment> getDeepLTranslations(DeepLClient client, List<TranslateRequest.Segment> texts, String targetLocale) {
        try {
            List<String> translatedTexts = client.translateText(texts.stream().map(TranslateRequest.Segment::text).toList(), null, targetLocale).stream().map(TextResult::getText).toList();
            List<TranslateResponse.TranslatedSegment> translatedSegments = new ArrayList<>();

            for (int i = 0; i < translatedTexts.size(); i++) {
                String translatedText = translatedTexts.get(i);
                TranslateRequest.Segment segment = texts.get(i);
                translatedSegments.add(new TranslateResponse.TranslatedSegment(
                        segment.idx(),
                        segment.text(),
                        translatedText,
                        segment.metadata()
                ));
            }

            return translatedSegments;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public record AsyncJobResult(TranslateResponse translateResponse, String failureDetail) {
    }

}
