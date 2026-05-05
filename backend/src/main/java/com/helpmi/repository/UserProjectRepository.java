package com.helpmi.repository;

import com.helpmi.domain.User;
import com.helpmi.domain.UserProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserProjectRepository extends JpaRepository<UserProject, UUID> {
    Optional<UserProject> findByUserIdAndProjectId(UUID userId, UUID projectId);

    @Query("SELECT up.user FROM UserProject up WHERE up.project.id = :projectId AND up.role = :role")
    List<User> findUsersByProjectIdAndRole(@Param("projectId") UUID projectId, @Param("role") String role);
}
