package com.example.aidemo.rag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "knowledge_document")
public class KnowledgeDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(length = 64)
    private String docId;

    @Column(columnDefinition = "CLOB")
    private String fullText;

    private Integer tokenEstimate;

    @Column(length = 32)
    private String parseEngine;

    @Column(length = 16)
    private String parseQuality;

    @Column(length = 32)
    private String documentType;

    @Column(length = 32)
    private String ingestStatus;

    private boolean degraded;

    @Column(length = 512)
    private String errorMessage;

    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getFullText() {
        return fullText;
    }

    public void setFullText(String fullText) {
        this.fullText = fullText;
    }

    public Integer getTokenEstimate() {
        return tokenEstimate;
    }

    public void setTokenEstimate(Integer tokenEstimate) {
        this.tokenEstimate = tokenEstimate;
    }

    public String getParseEngine() {
        return parseEngine;
    }

    public void setParseEngine(String parseEngine) {
        this.parseEngine = parseEngine;
    }

    public String getParseQuality() {
        return parseQuality;
    }

    public void setParseQuality(String parseQuality) {
        this.parseQuality = parseQuality;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getIngestStatus() {
        return ingestStatus;
    }

    public void setIngestStatus(String ingestStatus) {
        this.ingestStatus = ingestStatus;
    }

    public boolean isDegraded() {
        return degraded;
    }

    public void setDegraded(boolean degraded) {
        this.degraded = degraded;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
