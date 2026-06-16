package com.example.aidemo.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(@NotBlank String question, String sessionId) {}
