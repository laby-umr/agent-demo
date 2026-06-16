package com.example.aidemo.rag.model;

public class KnowledgeChunk {

    private String content;
    private String embedText;
    private BlockType blockType = BlockType.TEXT;
    private ChunkLevel chunkLevel = ChunkLevel.CHILD;
    private String tempKey;
    private String parentTempKey;
    private String headingPath;
    private Integer pageStart;
    private Integer pageEnd;
    private boolean embedEnabled = true;

    public String content() {
        return content;
    }

    public KnowledgeChunk content(String content) {
        this.content = content;
        return this;
    }

    public String embedText() {
        return embedText;
    }

    public KnowledgeChunk embedText(String embedText) {
        this.embedText = embedText;
        return this;
    }

    public BlockType blockType() {
        return blockType;
    }

    public KnowledgeChunk blockType(BlockType blockType) {
        this.blockType = blockType;
        return this;
    }

    public ChunkLevel chunkLevel() {
        return chunkLevel;
    }

    public KnowledgeChunk chunkLevel(ChunkLevel chunkLevel) {
        this.chunkLevel = chunkLevel;
        return this;
    }

    public String tempKey() {
        return tempKey;
    }

    public KnowledgeChunk tempKey(String tempKey) {
        this.tempKey = tempKey;
        return this;
    }

    public String parentTempKey() {
        return parentTempKey;
    }

    public KnowledgeChunk parentTempKey(String parentTempKey) {
        this.parentTempKey = parentTempKey;
        return this;
    }

    public String headingPath() {
        return headingPath;
    }

    public KnowledgeChunk headingPath(String headingPath) {
        this.headingPath = headingPath;
        return this;
    }

    public Integer pageStart() {
        return pageStart;
    }

    public KnowledgeChunk pageStart(Integer pageStart) {
        this.pageStart = pageStart;
        return this;
    }

    public Integer pageEnd() {
        return pageEnd;
    }

    public KnowledgeChunk pageEnd(Integer pageEnd) {
        this.pageEnd = pageEnd;
        return this;
    }

    public boolean embedEnabled() {
        return embedEnabled;
    }

    public KnowledgeChunk embedEnabled(boolean embedEnabled) {
        this.embedEnabled = embedEnabled;
        return this;
    }
}
