package com.abhishek.codereview.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewRequest {

    @NotBlank(message = "owner is required")
    private String owner;

    @NotBlank(message = "repo is required")
    private String repo;

    @NotNull(message = "pullRequestNumber is required")
    private Integer pullRequestNumber;
}