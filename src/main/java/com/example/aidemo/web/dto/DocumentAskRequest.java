package com.example.aidemo.web.dto;

import jakarta.validation.constraints.NotBlank;

public record DocumentAskRequest(@NotBlank String question, String sessionId) {}
