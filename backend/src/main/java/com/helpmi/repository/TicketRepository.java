package com.helpmi.repository;

import com.helpmi.domain.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

import java.util.List;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    @Query("""
        SELECT t FROM Ticket t
        WHERE t.project.id = :projectId
        AND (:statusCount = 0 OR t.status IN :statuses)
        AND (:priorityCount = 0 OR t.priority IN :priorities)
        AND (:typeCount = 0 OR t.type IN :types)
        AND (:assigneeId IS NULL OR t.assignee.id = :assigneeId)
        """)
    Page<Ticket> findByProjectIdWithFilters(
            @Param("projectId") UUID projectId,
            @Param("statuses") Collection<String> statuses,
            @Param("statusCount") int statusCount,
            @Param("priorities") Collection<String> priorities,
            @Param("priorityCount") int priorityCount,
            @Param("types") Collection<String> types,
            @Param("typeCount") int typeCount,
            @Param("assigneeId") UUID assigneeId,
            Pageable pageable);

    long countByProjectId(UUID projectId);
    long countByStatus(String status);
    long countByPriority(String priority);
    long countByType(String type);

    @Query("SELECT t FROM Ticket t WHERE UPPER(t.reference) LIKE :q OR UPPER(t.title) LIKE :q ORDER BY t.reference")
    List<Ticket> searchByQuery(@Param("q") String q, Pageable pageable);

    @Query("SELECT t FROM Ticket t WHERE (UPPER(t.reference) LIKE :q OR UPPER(t.title) LIKE :q) AND t.project.id IN :projectIds ORDER BY t.reference")
    List<Ticket> searchByQueryInProjects(@Param("q") String q, @Param("projectIds") List<UUID> projectIds, Pageable pageable);
}
