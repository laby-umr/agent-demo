package com.example.aidemo.web.dto;

import com.example.aidemo.document.model.Evidence;
import java.util.List;

public record DocumentQaResponse(
        String answer,
        String confidence,
        String refusalReason,
        List<Evidence> evidences,
        String sessionId) {}
