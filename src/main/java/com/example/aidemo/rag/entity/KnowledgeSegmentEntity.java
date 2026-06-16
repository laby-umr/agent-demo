package com.example.aidemo.rag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "knowledge_segment",
        indexes = {
            @Index(name = "idx_segment_document", columnList = "documentId"),
            @Index(name = "idx_segment_vector", columnList = "vectorId")
        })
public class KnowledgeSegmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long documentId;

    @Column(columnDefinition = "CLOB")
    private String content;

    @Column(columnDefinition = "CLOB")
    private String embedText;

    @Column(columnDefinition = "CLOB")
    private String sparseText;

    @Column(length = 32)
    private String blockType;

    @Column(length = 16)
    private String chunkLevel;

    private Long parentId;

    @Column(length = 512)
    private String headingPath;

    private Integer pageStart;
    private Integer pageEnd;

    @Column(length = 64)
    private String vectorId;

    private boolean embedEnabled = true;
    private int retrievalCount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getEmbedText() {
        return embedText;
    }

    public void setEmbedText(String embedText) {
        this.embedText = embedText;
    }

    public String getSparseText() {
        return sparseText;
    }

    public void setSparseText(String sparseText) {
        this.sparseText = sparseText;
    }

    public String getBlockType() {
        return blockType;
    }

    public void setBlockType(String blockType) {
        this.blockType = blockType;
    }

    public String getChunkLevel() {
        return chunkLevel;
    }

    public void setChunkLevel(String chunkLevel) {
        this.chunkLevel = chunkLevel;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getHeadingPath() {
        return headingPath;
    }

    public void setHeadingPath(String headingPath) {
        this.headingPath = headingPath;
    }

    public Integer getPageStart() {
        return pageStart;
    }

    public void setPageStart(Integer pageStart) {
        this.pageStart = pageStart;
    }

    public Integer getPageEnd() {
        return pageEnd;
    }

    public void setPageEnd(Integer pageEnd) {
        this.pageEnd = pageEnd;
    }

    public String getVectorId() {
        return vectorId;
    }

    public void setVectorId(String vectorId) {
        this.vectorId = vectorId;
    }

    public boolean isEmbedEnabled() {
        return embedEnabled;
    }

    public void setEmbedEnabled(boolean embedEnabled) {
        this.embedEnabled = embedEnabled;
    }

    public int getRetrievalCount() {
        return retrievalCount;
    }

    public void setRetrievalCount(int retrievalCount) {
        this.retrievalCount = retrievalCount;
    }
}
