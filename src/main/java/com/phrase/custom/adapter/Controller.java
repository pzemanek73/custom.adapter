package com.phrase.custom.adapter;

import com.phrase.custom.adapter.dto.Locale;
import com.phrase.custom.adapter.dto.request.LanguagesRequest;
import com.phrase.custom.adapter.dto.request.StatusRequest;
import com.phrase.custom.adapter.dto.request.TranslateRequest;
import com.phrase.custom.adapter.dto.response.ErrorResponse;
import com.phrase.custom.adapter.dto.response.LanguagesResponse;
import com.phrase.custom.adapter.dto.response.StatusResponse;
import com.phrase.custom.adapter.dto.response.TranslateResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

@RestController
public class Controller {

    @PostMapping("/languages")
    public ResponseEntity<LanguagesResponse> languages(@RequestBody LanguagesRequest languagesRequest) {
        LanguagesResponse languagesResponse = new LanguagesResponse(List.of(
                new LanguagesResponse.LanguagePair(new Locale("en"), new Locale("de")),
                new LanguagesResponse.LanguagePair(new Locale("en"), new Locale("cs"))
        ));

        return new ResponseEntity<>(languagesResponse, OK);
    }

    @PostMapping("/status")
    public ResponseEntity<StatusResponse> status(@RequestBody StatusRequest statusRequest) {
        StatusResponse statusResponse = new StatusResponse(StatusResponse.Status.OK);

        return new ResponseEntity<>(statusResponse, OK);
    }

    @PostMapping("/translate")
    public ResponseEntity<TranslateResponse> translate(@RequestBody TranslateRequest translateRequest) {
        List<TranslateResponse.TranslatedSegment> translatedSegments = translateRequest.segments().stream()
                .map(s -> new TranslateResponse.TranslatedSegment(
                                s.idx(),
                                s.text(),
                                "%s [%s]".formatted(s.text(), translateRequest.targetLanguage().locale().equals("de") ? "german" : "czech"),
                                s.metadata()
                        )
                )
                .toList();

        TranslateResponse translateResponse = new TranslateResponse(
                translateRequest.sourceLanguage(),
                translateRequest.targetLanguage(),
                translatedSegments,
                translateRequest.metadata()
        );

        return new ResponseEntity<>(translateResponse, OK);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleErrors(Exception exception) {
        ErrorResponse errorResponse = new ErrorResponse("Application Error: %s, %s".formatted(exception.getMessage(), exception.getCause()));
        return new ResponseEntity<>(errorResponse, INTERNAL_SERVER_ERROR);
    }

}
