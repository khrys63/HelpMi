package com.helpmi.repository;

import com.helpmi.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
