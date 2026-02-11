package com.phrase.custom.adapter;

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
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.time.Instant.now;
import static java.util.Collections.list;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.joining;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

@RestController
public class Controller {

    private final Logger logger = LoggerFactory.getLogger(Controller.class);

    private static final Cache<@NotNull String, Pair<Instant, TranslateRequest>> translateRequestCache =
            Caffeine.<String, TranslateRequest>newBuilder()
                    .expireAfterWrite(15, TimeUnit.MINUTES)
                    .build();

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

        return new ResponseEntity<>(languagesResponse, OK);
    }

    @PostMapping("/status")
    public ResponseEntity<StatusResponse> status(@RequestBody StatusRequest statusRequest, HttpServletRequest request) {
        logger.info("Status request: {}", statusRequest);
        processHeaders(request);

        // If the engine isn't fully ready, return NOT_OK

        StatusResponse statusResponse = new StatusResponse(StatusResponse.Status.OK);
        return new ResponseEntity<>(statusResponse, OK);
    }

    @PostMapping("/translate")
    public ResponseEntity<TranslateResponse> translate(@RequestBody TranslateRequest translateRequest, HttpServletRequest request) {
        logger.info("Translate request: {}", translateRequest);
        processHeaders(request);

        // Call your engine here
        // Please make sure to be able to handle up to 500 segments

        TranslateResponse translateResponse = getTranslateResponse(translateRequest);
        return new ResponseEntity<>(translateResponse, OK);
    }

    @PostMapping("/translateAsync")
    public ResponseEntity<TranslateAsyncResponse> translateAsync(@RequestBody TranslateRequest translateRequest, HttpServletRequest request) {
        logger.info("Translate async request: {}", translateRequest);
        processHeaders(request);

        String jobId = UUID.randomUUID().toString();
        translateRequestCache.put(jobId, Pair.of(now(), translateRequest));

        // Call your engine here - start the asynchronous translation

        // Please make sure to be able to handle up to 500 segments

        // Note that this endpoint should return fast and be non-blocking (do not call the engine directly here,
        // for example, use a separate thread or a processing queue to start the async. translation)
        // If your engine doesn't support async processing, you can still do it similarly to the sync call, but accumulate the results and return them asynchronously
        // Be careful in regard to concurrency, make sure the jobs are not overwriting each other's data, are thread-safe
        // and separated by the jobId

        logger.info("Starting jobId: '{}'", jobId);

        TranslateAsyncResponse translateAsyncResponse = new TranslateAsyncResponse(jobId);
        return new ResponseEntity<>(translateAsyncResponse, OK);
    }

    @GetMapping("/translateAsyncStatus/{jobId}")
    public ResponseEntity<TranslateAsyncStatusResponse> translateAsyncStatus(@PathVariable String jobId, HttpServletRequest request) {
        logger.info("Translate async status request: {}", jobId);
        processHeaders(request);

        // Report the progress of the translation

        Pair<Instant, TranslateRequest> cacheRecord = translateRequestCache.getIfPresent(jobId);
        checkCacheRecord(jobId, cacheRecord);

        // Simulating work for 5 sec
        TranslateAsyncStatusResponse translateAsyncStatusResponse = new TranslateAsyncStatusResponse(
                now().isBefore(cacheRecord.getLeft().plusSeconds(5)) ? AsyncStatus.RUNNING : AsyncStatus.DONE,
                "detail" // In case of a FAILED status, pls include some detail explanation
        );

        return new ResponseEntity<>(translateAsyncStatusResponse, OK);
    }

    @GetMapping("/translateAsyncResult/{jobId}")
    public ResponseEntity<TranslateResponse> translateAsyncResult(@PathVariable String jobId, HttpServletRequest request) {
        logger.info("Translate async result request: {}", jobId);
        processHeaders(request);

        Pair<Instant, TranslateRequest> cacheRecord = translateRequestCache.getIfPresent(jobId);
        checkCacheRecord(jobId, cacheRecord);

        TranslateResponse translateResponse = getTranslateResponse(cacheRecord.getRight());
        return new ResponseEntity<>(translateResponse, OK);
    }

    private void processHeaders(HttpServletRequest request) {
        // Can authenticate here, throw an exception when not authenticated

        String headers = list(request.getHeaderNames())
                .stream()
                .flatMap(headerName ->
                        list(request.getHeaders(headerName))
                                .stream()
                                .map(headerValue -> "%s: %s".formatted(headerName, headerValue))
                ).collect(joining(", "));

        logger.info("Http headers: {}", headers);
    }

    private void checkCacheRecord(String jobId, Pair<Instant, TranslateRequest> cacheRecord) {
        if (isNull(cacheRecord)) {
            throw new IllegalStateException("No translation job found with id '%s'".formatted(jobId));
        }
    }

    private @NotNull TranslateResponse getTranslateResponse(TranslateRequest translateRequest) {
        // Translation simulation loopback (it only adds the target locale to the input segments)
        List<TranslateResponse.TranslatedSegment> translatedSegments = translateRequest.segments().stream()
                .map(s -> new TranslateResponse.TranslatedSegment(
                                s.idx(),
                                s.text(),
                                "%s [%s]".formatted(s.text(), translateRequest.targetLanguage().locale()),
                                s.metadata()
                        )
                )
                .toList();

        return new TranslateResponse(
                translateRequest.sourceLanguage(),
                translateRequest.targetLanguage(),
                translatedSegments,
                translateRequest.metadata()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleErrors(Exception exception) {
        ErrorResponse errorResponse = new ErrorResponse("Application Error: %s, %s".formatted(exception.getMessage(), exception.getCause()));
        return new ResponseEntity<>(errorResponse, INTERNAL_SERVER_ERROR);
    }

}
