package com.example.aidemo.rag.repository;

import com.example.aidemo.rag.entity.KnowledgeSegmentEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KnowledgeSegmentRepository extends JpaRepository<KnowledgeSegmentEntity, Long> {

    List<KnowledgeSegmentEntity> findByDocumentId(Long documentId);

    List<KnowledgeSegmentEntity> findByIdIn(Collection<Long> ids);

    List<KnowledgeSegmentEntity> findByVectorIdIn(Collection<String> vectorIds);

    Optional<KnowledgeSegmentEntity> findFirstByDocumentIdAndBlockTypeAndHeadingPath(
            Long documentId, String blockType, String headingPath);

    @Query(
            """
            select s from KnowledgeSegmentEntity s
            where lower(s.sparseText) like lower(concat('%', :term, '%'))
               or lower(s.embedText) like lower(concat('%', :term, '%'))
               or lower(s.content) like lower(concat('%', :term, '%'))
            """)
    List<KnowledgeSegmentEntity> searchByTerm(@Param("term") String term);

    @Modifying
    @Query("update KnowledgeSegmentEntity s set s.retrievalCount = s.retrievalCount + 1 where s.id in :ids")
    void incrementRetrievalCount(@Param("ids") Collection<Long> ids);

    void deleteByDocumentId(Long documentId);
}
