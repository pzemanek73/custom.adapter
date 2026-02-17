package com.phrase.custom.adapter.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phrase.custom.adapter.dto.Locale;
import com.phrase.custom.adapter.dto.request.TranslateRequest;
import com.phrase.custom.adapter.dto.request.TranslateRequest.Segment;
import com.phrase.custom.adapter.dto.response.TranslateResponse;
import com.phrase.custom.adapter.service.TranslationService;
import com.phrase.custom.adapter.service.TranslationService.AsyncJobResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(Controller.class)
class ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TranslationService translationService;

    @Test
    void languagesReturnsConfiguredLanguagePairs() throws Exception {
        mockMvc.perform(post("/languages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metadata\":{\"formality\":\"informal\"}}")
                        .header("X-Test", "value"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languagePairs.length()").value(3))
                .andExpect(jsonPath("$.languagePairs[0].sourceLanguage").value("en"))
                .andExpect(jsonPath("$.languagePairs[0].targetLanguage").value("de"));
    }

    @Test
    void statusReturnsOk() throws Exception {
        mockMvc.perform(post("/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metadata\":{\"formality\":\"informal\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void translateDelegatesToServiceAndReturnsPayload() throws Exception {
        TranslateResponse translateResponse = sampleTranslateResponse();
        when(translationService.translate(any(TranslateRequest.class))).thenReturn(translateResponse);

        mockMvc.perform(post("/translate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleTranslateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceLanguage").value("en"))
                .andExpect(jsonPath("$.targetLanguage").value("de"))
                .andExpect(jsonPath("$.segments[0].translatedText").value("Hello [de]"));

        verify(translationService).translate(any(TranslateRequest.class));
    }

    @Test
    void translateAsyncStatusReturnsRunningWhenJobNotDone() throws Exception {
        when(translationService.translateAsync(any(TranslateRequest.class))).thenReturn(new CompletableFuture<>());

        String jobId = startAsyncJob();

        mockMvc.perform(get("/translateAsyncStatus/{jobId}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("running"))
                .andExpect(jsonPath("$.detail").value("no detail"));
    }

    @Test
    void translateAsyncStatusAndResultReturnDoneWhenJobCompleted() throws Exception {
        TranslateResponse translateResponse = sampleTranslateResponse();
        when(translationService.translateAsync(any(TranslateRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(new AsyncJobResult(translateResponse, null)));

        String jobId = startAsyncJob();

        mockMvc.perform(get("/translateAsyncStatus/{jobId}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("done"))
                .andExpect(jsonPath("$.detail").value("completed successfully"));

        mockMvc.perform(get("/translateAsyncResult/{jobId}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.segments[0].translatedText").value("Hello [de]"));
    }

    @Test
    void translateAsyncStatusReturnsFailedWhenJobCompletedWithError() throws Exception {
        when(translationService.translateAsync(any(TranslateRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(new AsyncJobResult(null, "upstream timeout")));

        String jobId = startAsyncJob();

        mockMvc.perform(get("/translateAsyncStatus/{jobId}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.detail").value("upstream timeout"));
    }

    @Test
    void translateAsyncStatusReturnsInternalServerErrorForUnknownJob() throws Exception {
        mockMvc.perform(get("/translateAsyncStatus/{jobId}", "missing-job"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error", containsString("No translation job found with id 'missing-job'")));
    }

    @Test
    void translateAsyncReturnsTooManyRequestsWhenExecutionRejected() throws Exception {
        when(translationService.translateAsync(any(TranslateRequest.class)))
                .thenThrow(new RejectedExecutionException("pool exhausted"));

        mockMvc.perform(post("/translateAsync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleTranslateRequest())))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error", containsString("Application busy: pool exhausted")));
    }

    private String startAsyncJob() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/translateAsync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleTranslateRequest())))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        return json.get("id").asText();
    }

    private TranslateRequest sampleTranslateRequest() {
        return new TranslateRequest(
                new Locale("en"),
                new Locale("de"),
                List.of(new Segment("1", "Hello", Map.of("origin", "unit-test"))),
                null,
                Map.of("requestId", "req-123")
        );
    }

    private TranslateResponse sampleTranslateResponse() {
        return new TranslateResponse(
                new Locale("en"),
                new Locale("de"),
                List.of(new TranslateResponse.TranslatedSegment("1", "Hello", "Hello [de]",
                        Map.of("origin", "unit-test"))),
                Map.of("requestId", "req-123")
        );
    }
}
