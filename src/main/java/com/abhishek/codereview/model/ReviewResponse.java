package com.abhishek.codereview.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewResponse {
    private int filesReviewed;
    private List<String> issuesFound;
    private String overallAssessment;
    private boolean commentsPosted;
}