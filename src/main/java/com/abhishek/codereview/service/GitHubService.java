package com.abhishek.codereview.service;

import com.abhishek.codereview.exception.GitHubApiException;
import com.abhishek.codereview.model.GitHubComment;
import com.abhishek.codereview.model.GitHubCommentRequest;
import com.abhishek.codereview.model.GitHubFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Slf4j
@Service
public class GitHubService {

    private final RestClient restClient;
    private final String token;

    public GitHubService(@Qualifier("gitHubRestClient") RestClient restClient,
                         @Value("${github.api.token}") String token) {
        this.restClient = restClient;
        this.token = token;
    }

    public List<GitHubFile> fetchPullRequestFiles(String owner, String repo, Integer prNumber) {
        try {
            log.info("Fetching PR files for {}/{} #{}", owner, repo, prNumber);

            List<GitHubFile> files = restClient.get()
                    .uri("/repos/{owner}/{repo}/pulls/{prNumber}/files", owner, repo, prNumber)
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<GitHubFile>>() {
                    });

            log.info("Fetched {} file(s) for PR #{}", files != null ? files.size() : 0, prNumber);
            return files != null ? files : List.of();

        } catch (RestClientResponseException ex) {
            log.error("GitHub API error while fetching PR files. status={}, body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            throw new GitHubApiException("Failed to fetch PR files: " + ex.getStatusCode(), ex);
        } catch (Exception ex) {
            log.error("Unexpected error while fetching PR files", ex);
            throw new GitHubApiException("Failed to communicate with GitHub API", ex);
        }
    }

    public void postReviewComment(String owner, String repo, Integer prNumber, String commentBody) {
        try {
            log.info("Posting review comment to {}/{} #{}", owner, repo, prNumber);

            GitHubCommentRequest request = new GitHubCommentRequest(commentBody);

            GitHubComment response = restClient.post()
                    .uri("/repos/{owner}/{repo}/issues/{prNumber}/comments", owner, repo, prNumber)
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .body(request)
                    .retrieve()
                    .body(GitHubComment.class);

            log.info("Posted comment successfully. commentId={}", response != null ? response.id() : null);

        } catch (RestClientResponseException ex) {
            log.error("GitHub API error while posting comment. status={}, body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            throw new GitHubApiException("Failed to post review comment: " + ex.getStatusCode(), ex);
        } catch (Exception ex) {
            log.error("Unexpected error while posting comment", ex);
            throw new GitHubApiException("Failed to communicate with GitHub API", ex);
        }
    }
}