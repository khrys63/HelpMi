package com.helpmi.repository;

import com.helpmi.domain.PersonalToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonalTokenRepository extends JpaRepository<PersonalToken, UUID> {
    List<PersonalToken> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<PersonalToken> findByTokenHash(String tokenHash);
}
