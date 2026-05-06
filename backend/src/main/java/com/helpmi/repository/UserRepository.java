package com.helpmi.repository;

import com.helpmi.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByKeycloakId(String keycloakId);
    List<User> findByActiveTrueOrderByFirstNameAscLastNameAsc();
    List<User> findAllByOrderByFirstNameAscLastNameAsc();
    @Query("SELECT u FROM User u JOIN u.organizations o WHERE o.id = :orgId ORDER BY u.firstName ASC, u.lastName ASC")
    List<User> findByOrganizationId(@Param("orgId") UUID orgId);

    @Query("SELECT COUNT(u) FROM User u JOIN u.organizations o WHERE o.id = :orgId")
    long countByOrganizationId(@Param("orgId") UUID orgId);

    @Query("""
        SELECT DISTINCT u FROM User u
        WHERE u.active = true
          AND EXISTS (
              SELECT 1 FROM UserProject up WHERE up.user = u AND up.project.id = :projectId
          )
        ORDER BY u.firstName ASC, u.lastName ASC
    """)
    List<User> findAssignableByProjectId(@Param("projectId") UUID projectId);

    @Query("""
        SELECT COUNT(u) > 0 FROM User u
        WHERE u.id = :userId AND u.active = true
          AND EXISTS (
              SELECT 1 FROM UserProject up WHERE up.user = u AND up.project.id = :projectId
          )
    """)
    boolean isAssignableToProject(@Param("userId") UUID userId, @Param("projectId") UUID projectId);

    @Query("""
        SELECT DISTINCT u FROM User u JOIN u.organizations o
        WHERE u.active = true AND o.id IN :orgIds
        ORDER BY u.firstName ASC, u.lastName ASC
    """)
    List<User> findByOrganizationIds(@Param("orgIds") Collection<UUID> orgIds);

    // A3-M3: users sharing at least one project with the given user
    @Query("""
        SELECT DISTINCT u FROM User u
        WHERE u.active = true
          AND EXISTS (
              SELECT 1 FROM UserProject up WHERE up.user = u AND up.project.id IN (
                  SELECT up2.project.id FROM UserProject up2 WHERE up2.user.id = :userId
              )
          )
        ORDER BY u.firstName ASC, u.lastName ASC
    """)
    List<User> findActiveUsersInSameProjects(@Param("userId") UUID userId);
}
