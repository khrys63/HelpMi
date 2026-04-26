package com.helpmi.repository;

import com.helpmi.domain.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    List<Organization> findByActiveTrueOrderByNameAsc();
    boolean existsByName(String name);

    @Query("SELECT o FROM Organization o LEFT JOIN FETCH o.projects WHERE o.id = :id")
    Optional<Organization> findByIdWithProjects(@Param("id") UUID id);
}
