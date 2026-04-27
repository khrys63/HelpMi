package com.helpmi.repository;

import com.helpmi.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByKeycloakId(String keycloakId);
    List<User> findByActiveTrueOrderByFirstNameAscLastNameAsc();
    List<User> findAllByOrderByFirstNameAscLastNameAsc();
    List<User> findByOrganizationId(UUID organizationId);
    long countByOrganizationId(UUID organizationId);

    @Query("""
        SELECT DISTINCT u FROM User u
        WHERE u.active = true AND (
            u.role = com.helpmi.domain.enums.UserRole.ADMIN
            OR EXISTS (
                SELECT 1 FROM UserProject up WHERE up.user = u AND up.project.id = :projectId
            )
        )
        ORDER BY u.firstName ASC, u.lastName ASC
    """)
    List<User> findAssignableByProjectId(@Param("projectId") UUID projectId);

    @Query("""
        SELECT COUNT(u) > 0 FROM User u
        WHERE u.id = :userId AND u.active = true AND (
            u.role = com.helpmi.domain.enums.UserRole.ADMIN
            OR EXISTS (
                SELECT 1 FROM UserProject up WHERE up.user = u AND up.project.id = :projectId
            )
        )
    """)
    boolean isAssignableToProject(@Param("userId") UUID userId, @Param("projectId") UUID projectId);
}
