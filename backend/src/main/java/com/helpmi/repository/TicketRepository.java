package com.helpmi.repository;

import com.helpmi.domain.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    @Query("""
        SELECT t FROM Ticket t
        WHERE t.project.id = :projectId
        AND (:status IS NULL OR t.status = :status)
        AND (:priority IS NULL OR t.priority = :priority)
        AND (:type IS NULL OR t.type = :type)
        AND (:assigneeId IS NULL OR t.assignee.id = :assigneeId)
        """)
    Page<Ticket> findByProjectIdWithFilters(
            @Param("projectId") UUID projectId,
            @Param("status") String status,
            @Param("priority") String priority,
            @Param("type") String type,
            @Param("assigneeId") UUID assigneeId,
            Pageable pageable);

    long countByProjectId(UUID projectId);
    long countByStatus(String status);
    long countByPriority(String priority);
    long countByType(String type);

    @Query("SELECT t FROM Ticket t WHERE UPPER(t.reference) LIKE :q OR UPPER(t.title) LIKE :q ORDER BY t.reference")
    List<Ticket> searchByQuery(@Param("q") String q, Pageable pageable);
}
