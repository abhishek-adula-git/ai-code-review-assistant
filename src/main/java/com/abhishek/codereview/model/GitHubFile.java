package com.abhishek.codereview.model;

public record GitHubFile(
        String filename,
        String status,
        Integer additions,
        Integer deletions,
        Integer changes,
        String patch
) {
}