package com.abhishek.codereview.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class GitHubRestClientConfig {

    private static final String GITHUB_BASE_URL = "https://api.github.com";

    @Bean
    public RestClient gitHubRestClient() {
        return RestClient.builder()
                .baseUrl(GITHUB_BASE_URL)
                .build();
    }
}