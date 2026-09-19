package com.abhishek.codereview.model;

import java.util.List;

public record GeminiRequest(List<Content> contents, GenerationConfig generationConfig) {

    public record Content(String role, List<Part> parts) {
    }

    public record Part(String text) {
    }

    public record GenerationConfig(String responseMimeType) {
    }
}