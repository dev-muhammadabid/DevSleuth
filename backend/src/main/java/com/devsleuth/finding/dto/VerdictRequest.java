package com.devsleuth.finding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerdictRequest(
        @NotBlank @Pattern(regexp = "CONFIRMED|DISMISSED") String verdict
) {}
