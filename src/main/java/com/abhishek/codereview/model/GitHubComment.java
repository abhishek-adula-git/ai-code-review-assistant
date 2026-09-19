package com.abhishek.codereview.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubComment(
        Long id,
        String body,
        @JsonProperty("html_url") String htmlUrl
) {
}