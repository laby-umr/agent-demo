package com.example.aidemo.web.dto;

import jakarta.validation.constraints.NotBlank;

public record AddDocumentRequest(@NotBlank String content, String title) {}
