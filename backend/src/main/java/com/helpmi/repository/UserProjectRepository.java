package com.helpmi.repository;

import com.helpmi.domain.UserProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserProjectRepository extends JpaRepository<UserProject, UUID> {
    Optional<UserProject> findByUserIdAndProjectId(UUID userId, UUID projectId);
}
