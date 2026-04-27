package com.helpmi.repository;

import com.helpmi.domain.Project;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findByActiveTrueOrderByCreatedAtDesc();
    boolean existsByKey(String key);

    @Query("SELECT p FROM Project p JOIN p.organizations o WHERE o.id = :orgId AND p.active = true ORDER BY p.createdAt DESC")
    List<Project> findActiveByOrganizationId(@Param("orgId") UUID orgId);

    @Query("SELECT p.id FROM Project p JOIN p.organizations o WHERE o.id = :orgId AND p.active = true")
    List<UUID> findIdsByOrganizationId(@Param("orgId") UUID orgId);

    @Query("SELECT COUNT(p) > 0 FROM Project p JOIN p.organizations o WHERE p.id = :projectId AND o.id = :orgId")
    boolean isProjectInOrganization(@Param("projectId") UUID projectId, @Param("orgId") UUID orgId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Project p WHERE p.id = :id")
    Optional<Project> findByIdForUpdate(@Param("id") UUID id);

    @Query("SELECT up.project FROM UserProject up WHERE up.user.id = :userId AND up.project.active = true ORDER BY up.project.createdAt DESC")
    List<Project> findActiveByUserId(@Param("userId") UUID userId);

    @Query("SELECT up.project.id FROM UserProject up WHERE up.user.id = :userId AND up.project.active = true")
    List<UUID> findIdsByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(up) > 0 FROM UserProject up WHERE up.user.id = :userId AND up.project.id = :projectId AND up.project.active = true")
    boolean isProjectAccessibleToUser(@Param("projectId") UUID projectId, @Param("userId") UUID userId);
}
