package com.abhishek.codereview.service;

import com.abhishek.codereview.exception.GeminiApiException;
import com.abhishek.codereview.model.GeminiRequest;
import com.abhishek.codereview.model.GeminiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Service
public class GeminiService {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public GeminiService(@Qualifier("geminiRestClient") RestClient restClient,
                         @Value("${gemini.api.key}") String apiKey,
                         @Value("${gemini.model}") String model) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.model = model;
    }

    public String callGemini(String prompt) {
        GeminiRequest request = new GeminiRequest(
                List.of(new GeminiRequest.Content("user", List.of(new GeminiRequest.Part(prompt)))),
                new GeminiRequest.GenerationConfig("application/json"));

        try {
            log.info("Calling Gemini API. model={}, promptLength={}", model, prompt.length());

            GeminiResponse response = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GeminiResponse.class);

            log.debug("Raw response from Gemini: {}", response);

            if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
                throw new GeminiApiException("Gemini returned an empty response");
            }

            var candidate = response.candidates().getFirst();

            if (candidate.content() == null || candidate.content().parts() == null
                    || candidate.content().parts().isEmpty()) {
                throw new GeminiApiException("Gemini response does not contain text");
            }

            String text = candidate.content().parts().getFirst().text();
            log.info("Received content from Gemini. length={}", text != null ? text.length() : 0);

            if (text == null || text.isBlank()) {
                throw new GeminiApiException("Gemini returned empty text");
            }

            return text;

        } catch (GeminiApiException ex) {
            log.error("GeminiApiException occurred while calling Gemini API", ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected exception occurred while calling Gemini API", ex);
            throw new GeminiApiException("Failed to communicate with Gemini API", ex);
        }
    }
}