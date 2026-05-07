package com.helpmi.repository;

import com.helpmi.domain.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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

    // --- Dashboard queries ---

    @Query("SELECT t FROM Ticket t WHERE t.reporter.id = :userId AND t.project.archived = false AND t.status NOT IN :statuses ORDER BY t.updatedAt DESC")
    List<Ticket> findReportedByUserAndStatusNotIn(@Param("userId") UUID userId, @Param("statuses") List<String> statuses);

    @Query("SELECT t FROM Ticket t WHERE t.assignee.id = :userId AND t.project.archived = false AND t.status NOT IN :statuses ORDER BY t.updatedAt DESC")
    List<Ticket> findAssignedToUserAndStatusNotIn(@Param("userId") UUID userId, @Param("statuses") List<String> statuses);

    @Query("SELECT t FROM Ticket t JOIN t.watchers w WHERE w.id = :userId AND t.project.archived = false AND t.status NOT IN :statuses ORDER BY t.updatedAt DESC")
    List<Ticket> findWatchedByUserIdAndStatusNotIn(@Param("userId") UUID userId, @Param("statuses") List<String> statuses);

    @Query("""
        SELECT t FROM Ticket t
        WHERE t.project.id IN (SELECT up.project.id FROM UserProject up WHERE up.user.id = :userId)
        AND t.project.archived = false
        AND t.dueDate BETWEEN :from AND :to
        AND t.status NOT IN :statuses
        ORDER BY t.dueDate ASC
        """)
    List<Ticket> findDueSoonForUser(@Param("userId") UUID userId, @Param("from") LocalDate from,
                                    @Param("to") LocalDate to, @Param("statuses") List<String> statuses);

    @Query("""
        SELECT t.project.id, t.project.key, t.project.name, t.status, COUNT(t)
        FROM Ticket t
        WHERE t.project.id IN (SELECT up.project.id FROM UserProject up WHERE up.user.id = :userId)
        AND t.project.archived = false
        AND t.status IN :statuses
        GROUP BY t.project.id, t.project.key, t.project.name, t.status
        ORDER BY t.project.name ASC
        """)
    List<Object[]> countTicketsByProjectAndStatus(@Param("userId") UUID userId, @Param("statuses") List<String> statuses);

    // --- Manager tracking ---

    @Query("""
        SELECT t.project.id, t.project.key, t.project.name,
               t.assignee.id, t.assignee.firstName, t.assignee.lastName, t.assignee.email,
               t.status, COUNT(t)
        FROM Ticket t
        WHERE t.project.id IN (
            SELECT up.project.id FROM UserProject up
            WHERE up.user.id = :userId AND up.role = 'MANAGER'
        )
        AND t.project.archived = false
        AND t.status NOT IN ('CLOSED', 'CANCELLED')
        GROUP BY t.project.id, t.project.key, t.project.name,
                 t.assignee.id, t.assignee.firstName, t.assignee.lastName, t.assignee.email, t.status
        ORDER BY t.project.name ASC, t.assignee.lastName ASC
        """)
    List<Object[]> countTicketsByProjectAndAssignee(@Param("userId") UUID userId);

    @Query("""
        SELECT t FROM Ticket t
        WHERE t.project.id = :projectId
        AND t.assignee.id = :assigneeId
        AND t.status NOT IN ('CLOSED', 'CANCELLED')
        ORDER BY t.status, t.dueDate ASC, t.reference ASC
        """)
    List<Ticket> findByProjectManagerAndAssigneeId(
            @Param("projectId") UUID projectId, @Param("assigneeId") UUID assigneeId);

    @Query("""
        SELECT t FROM Ticket t
        WHERE t.project.id = :projectId
        AND t.assignee IS NULL
        AND t.status NOT IN ('CLOSED', 'CANCELLED')
        ORDER BY t.dueDate ASC, t.reference ASC
        """)
    List<Ticket> findUnassignedTicketsForProject(@Param("projectId") UUID projectId);
}
