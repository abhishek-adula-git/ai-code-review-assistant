package com.abhishek.codereview.service;

import com.abhishek.codereview.model.GitHubFile;
import com.abhishek.codereview.model.ReviewRequest;
import com.abhishek.codereview.model.ReviewResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class CodeReviewService {

    private final GitHubService gitHubService;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    public CodeReviewService(GitHubService gitHubService, GeminiService geminiService, ObjectMapper objectMapper) {
        this.gitHubService = gitHubService;
        this.geminiService = geminiService;
        this.objectMapper = objectMapper;
    }

    public ReviewResponse reviewPullRequest(ReviewRequest request) {
        log.info("Starting review for {}/{} PR #{}", request.getOwner(), request.getRepo(), request.getPullRequestNumber());

        List<GitHubFile> files = gitHubService.fetchPullRequestFiles(
                request.getOwner(), request.getRepo(), request.getPullRequestNumber());

        if (files.isEmpty()) {
            log.info("No changed files found for PR #{}", request.getPullRequestNumber());
            return new ReviewResponse(0, List.of(), "No changed files found in this pull request.", false);
        }

        List<String> allIssues = new ArrayList<>();
        int reviewedCount = 0;

        for (GitHubFile file : files) {
            if (file.patch() == null || file.patch().isBlank()) {
                log.debug("Skipping file with no diff content: {}", file.filename());
                continue;
            }

            reviewedCount++;
            String prompt = buildPrompt(file.filename(), file.patch());
            String rawLlmOutput = geminiService.callGemini(prompt);
            LlmReviewResult parsed = parseLlmOutput(rawLlmOutput, file.filename());

            if (parsed.hasIssues()) {
                for (String issue : parsed.issues()) {
                    allIssues.add(file.filename() + ": " + issue);
                }
            }
        }

        String overallAssessment = buildOverallAssessment(reviewedCount, allIssues);
        log.info("Review complete. filesReviewed={}, issuesFound={}", reviewedCount, allIssues.size());

        String commentBody = formatAsMarkdown(overallAssessment, allIssues);
        gitHubService.postReviewComment(
                request.getOwner(), request.getRepo(), request.getPullRequestNumber(), commentBody);

        return new ReviewResponse(reviewedCount, allIssues, overallAssessment, true);
    }

    private String buildPrompt(String filename, String diffPatch) {
        return """
                You are a senior software engineer performing a code review.

                Review the following diff from file '%s'.

                Flag only genuine concerns:
                - Potential bugs or logic errors
                - Missing null checks or error handling
                - Missing test coverage for new logic
                - Security issues

                Do NOT comment on formatting, whitespace, or style preferences.
                Do NOT invent issues that are not evident from the diff.

                Return ONLY valid JSON with exactly these fields:
                {
                  "hasIssues": true or false,
                  "issues": ["short issue description", "..."]
                }

                Diff:
                %s
                """.formatted(filename, diffPatch);
    }

    private LlmReviewResult parseLlmOutput(String rawText, String filename) {
        try {
            String cleaned = cleanJson(rawText);
            LlmReviewResult result = objectMapper.readValue(cleaned, LlmReviewResult.class);
            log.debug("Parsed review result for {}: {}", filename, result);
            return result;
        } catch (Exception ex) {
            log.error("Failed to parse Gemini response for file {}. rawOutput={}", filename, rawText, ex);
            throw new IllegalArgumentException("Unable to parse Gemini response for file: " + filename, ex);
        }
    }

    private String cleanJson(String text) {
        String cleaned = text.trim();

        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        return cleaned.trim();
    }

    private String buildOverallAssessment(int reviewedCount, List<String> issues) {
        if (issues.isEmpty()) {
            return "Reviewed " + reviewedCount + " file(s). No significant issues found.";
        }
        return "Reviewed " + reviewedCount + " file(s). Found " + issues.size() + " potential issue(s) to address.";
    }

    private String formatAsMarkdown(String overallAssessment, List<String> issues) {
        StringBuilder sb = new StringBuilder();
        sb.append("### AI Code Review\n\n");
        sb.append(overallAssessment).append("\n\n");

        if (!issues.isEmpty()) {
            sb.append("**Issues found:**\n");
            for (String issue : issues) {
                sb.append("- ").append(issue).append("\n");
            }
        }

        return sb.toString();
    }

    private record LlmReviewResult(boolean hasIssues, List<String> issues) {
    }
}