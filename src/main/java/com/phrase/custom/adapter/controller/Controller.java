package com.phrase.custom.adapter.controller;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.phrase.custom.adapter.dto.Locale;
import com.phrase.custom.adapter.dto.request.LanguagesRequest;
import com.phrase.custom.adapter.dto.request.StatusRequest;
import com.phrase.custom.adapter.dto.request.TranslateRequest;
import com.phrase.custom.adapter.dto.response.ErrorResponse;
import com.phrase.custom.adapter.dto.response.LanguagesResponse;
import com.phrase.custom.adapter.dto.response.LanguagesResponse.LanguagePair;
import com.phrase.custom.adapter.dto.response.StatusResponse;
import com.phrase.custom.adapter.dto.response.TranslateAsyncResponse;
import com.phrase.custom.adapter.dto.response.TranslateAsyncStatusResponse;
import com.phrase.custom.adapter.dto.response.TranslateAsyncStatusResponse.AsyncStatus;
import com.phrase.custom.adapter.dto.response.TranslateResponse;
import com.phrase.custom.adapter.service.TranslationService;
import com.phrase.custom.adapter.service.TranslationService.AsyncJobResult;
import jakarta.servlet.http.HttpServletRequest;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static com.phrase.custom.adapter.dto.response.TranslateAsyncStatusResponse.AsyncStatus.DONE;
import static com.phrase.custom.adapter.dto.response.TranslateAsyncStatusResponse.AsyncStatus.FAILED;
import static com.phrase.custom.adapter.dto.response.TranslateAsyncStatusResponse.AsyncStatus.RUNNING;
import static java.util.Collections.list;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

/**
 * Please make sure to implement all the endpoints in a meaningful way, especially the async endpoints.
 * Otherwise, the Phrase translation functions may not work as expected.
 */

@RestController
public class Controller {

    private final Logger logger = LoggerFactory.getLogger(Controller.class);

    private static final Cache<@NotNull String, CompletableFuture<AsyncJobResult>> asyncJobCache =
            Caffeine.newBuilder()
                    .expireAfterWrite(35, TimeUnit.MINUTES)
                    .build();

    @Autowired
    private TranslationService translationService;

    @PostMapping("/languages")
    public ResponseEntity<LanguagesResponse> languages(@RequestBody LanguagesRequest languagesRequest, HttpServletRequest request) {
        logger.info("Languages request: {}", languagesRequest);
        processHeaders(request);

        // Note that if your supported set is large (maybe even ALL the codes in the Locale enum),
        // the cartesian product would produce a very large response, >100MB
        // In that case please use just the top codes such as "en" for all the "en_gb", "en_us", etc. variants
        // Phrase interprets the top codes as including all the sub-locales
        // That way the size will be much smaller and manageable
        LanguagesResponse languagesResponse = new LanguagesResponse(List.of(
                new LanguagePair(new Locale("en"), new Locale("de")),
                new LanguagePair(new Locale("en"), new Locale("cs")),
                new LanguagePair(new Locale("en"), new Locale("zh_tw"))
        ));

        return ResponseEntity.ok(languagesResponse);
    }

    @PostMapping("/status")
    public ResponseEntity<StatusResponse> status(@RequestBody StatusRequest statusRequest, HttpServletRequest request) {
        logger.info("Status request: {}", statusRequest);
        processHeaders(request);

        // If the engine isn't fully ready, return NOT_OK

        StatusResponse statusResponse = new StatusResponse(StatusResponse.Status.OK);
        return ResponseEntity.ok(statusResponse);
    }

    @PostMapping("/translate")
    public ResponseEntity<TranslateResponse> translate(@RequestBody TranslateRequest translateRequest, HttpServletRequest request) {
        logger.info("Translate request: {}", translateRequest);
        processHeaders(request);

        // Call your engine here via the translation service
        // Please make sure to be able to handle up to 500 segments

        TranslateResponse translateResponse = translationService.translate(translateRequest);
        return ResponseEntity.ok(translateResponse);
    }

    @PostMapping("/translateAsync")
    public ResponseEntity<TranslateAsyncResponse> translateAsync(@RequestBody TranslateRequest translateRequest, HttpServletRequest request) {
        logger.info("Translate async request: {}", translateRequest);
        processHeaders(request);

        String jobId = UUID.randomUUID().toString();
        logger.info("Starting jobId: '{}'", jobId);

        // Call your engine via the translation service - start the asynchronous translation
        // Please make sure to be able to handle up to 500 segments

        // Note that this endpoint should return fast and be non-blocking (do not call the engine directly here,
        // for example, use a separate thread as demonstrated or a processing queue to start the async. translation)
        // If your engine doesn't support async processing, you can still call the engine synchronously in the translationService.translateAsync method
        // Be careful in regard to concurrency, make sure the jobs are not overwriting each other's data, are thread-safe
        // and separated by the jobId

        CompletableFuture<AsyncJobResult> future = translationService.translateAsync(translateRequest);
        asyncJobCache.put(jobId, future);

        TranslateAsyncResponse translateAsyncResponse = new TranslateAsyncResponse(jobId);
        return ResponseEntity.ok(translateAsyncResponse);
    }

    @GetMapping("/translateAsyncStatus/{jobId}")
    public ResponseEntity<TranslateAsyncStatusResponse> translateAsyncStatus(@PathVariable String jobId, HttpServletRequest request) throws ExecutionException, InterruptedException {
        logger.info("Translate async status request: {}", jobId);
        processHeaders(request);

        // Report the progress of the translation job

        CompletableFuture<AsyncJobResult> cacheRecord = getJobCacheRecord(jobId);

        AsyncStatus status = RUNNING;
        String detail = "no detail";
        if (cacheRecord.isDone()) {
            if (nonNull(cacheRecord.get().translateResponse())) {
                status = DONE;
                detail = "completed successfully";
            } else {
                status = FAILED;
                detail = cacheRecord.get().failureDetail(); // Make sure the failure detail is filled out as it gets propagated to the UI and is useful for debugging
            }
        }
        TranslateAsyncStatusResponse translateAsyncStatusResponse = new TranslateAsyncStatusResponse(status, detail);

        return ResponseEntity.ok(translateAsyncStatusResponse);
    }

    @GetMapping("/translateAsyncResult/{jobId}")
    public ResponseEntity<TranslateResponse> translateAsyncResult(@PathVariable String jobId, HttpServletRequest request) throws ExecutionException, InterruptedException {
        logger.info("Translate async result request: {}", jobId);
        processHeaders(request);

        CompletableFuture<AsyncJobResult> cacheRecord = getJobCacheRecord(jobId);

        TranslateResponse translateResponse = cacheRecord.get().translateResponse();
        return ResponseEntity.ok(translateResponse);
    }

    private void processHeaders(HttpServletRequest request) {
        // Can authenticate here, throw an exception when not authenticated
        boolean authenticated = true;
        if (!authenticated) {
            throw new IllegalStateException("Not authenticated");
        }

        String headers = list(request.getHeaderNames())
                .stream()
                .flatMap(headerName ->
                        list(request.getHeaders(headerName))
                                .stream()
                                .map(headerValue -> "%s: %s".formatted(headerName, headerValue))
                ).collect(joining(", "));

        logger.info("Http headers: {}", headers);
    }

    private CompletableFuture<AsyncJobResult> getJobCacheRecord(String jobId) {
        CompletableFuture<AsyncJobResult> cacheRecord = asyncJobCache.getIfPresent(jobId);
        if (isNull(cacheRecord)) {
            throw new IllegalStateException("No translation job found with id '%s'".formatted(jobId));
        }
        return cacheRecord;
    }

    @ExceptionHandler(RejectedExecutionException.class)
    public ResponseEntity<ErrorResponse> handleRejectedAsyncExecutionErrors(Exception exception) {
        // Backoff & retry
        ErrorResponse errorResponse = new ErrorResponse("Application busy: %s, %s".formatted(exception.getMessage(), exception.getCause()));
        return new ResponseEntity<>(errorResponse, TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOtherErrors(Exception exception) {
        ErrorResponse errorResponse = new ErrorResponse("Application error: %s, %s".formatted(exception.getMessage(), exception.getCause()));
        return ResponseEntity.internalServerError().body(errorResponse);
    }

}
