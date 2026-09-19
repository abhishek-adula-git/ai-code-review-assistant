package com.abhishek.codereview.controller;

import com.abhishek.codereview.model.ReviewRequest;
import com.abhishek.codereview.model.ReviewResponse;
import com.abhishek.codereview.service.CodeReviewService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/reviews")
public class CodeReviewController {

    private final CodeReviewService codeReviewService;

    public CodeReviewController(CodeReviewService codeReviewService) {
        this.codeReviewService = codeReviewService;
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> review(@Valid @RequestBody ReviewRequest request) {
        log.info("Received review request for {}/{} PR #{}",
                request.getOwner(), request.getRepo(), request.getPullRequestNumber());

        ReviewResponse response = codeReviewService.reviewPullRequest(request);

        log.info("Returning review response. filesReviewed={}, issuesFound={}",
                response.getFilesReviewed(), response.getIssuesFound().size());
        return ResponseEntity.ok(response);
    }
}