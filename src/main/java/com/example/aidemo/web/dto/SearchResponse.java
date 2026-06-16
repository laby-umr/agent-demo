package com.example.aidemo.web.dto;

import java.util.List;

public record SearchResponse(String query, List<SearchHit> hits) {

    public record SearchHit(String content, double score) {}
}
