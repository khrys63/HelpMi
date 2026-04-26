package com.helpmi.service;

import com.helpmi.domain.PersonalToken;
import com.helpmi.domain.User;
import com.helpmi.dto.request.CreatePersonalTokenRequest;
import com.helpmi.dto.response.PersonalTokenCreated;
import com.helpmi.dto.response.PersonalTokenResponse;
import com.helpmi.exception.ForbiddenException;
import com.helpmi.exception.NotFoundException;
import com.helpmi.repository.PersonalTokenRepository;
import com.helpmi.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PersonalTokenService {

    private final PersonalTokenRepository personalTokenRepository;
    private final CurrentUserService currentUserService;
    private final RateLimiterService rateLimiterService;

    @Transactional(readOnly = true)
    public List<PersonalTokenResponse> listTokens() {
        User user = currentUserService.getCurrentUser();
        return personalTokenRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::toResponse).toList();
    }

    public PersonalTokenCreated createToken(CreatePersonalTokenRequest req) {
        User user = currentUserService.getCurrentUser();
        rateLimiterService.checkTokenCreation(user.getId());
        String plain = generateToken();
        PersonalToken token = PersonalToken.builder()
                .user(user)
                .name(req.name())
                .tokenHash(sha256(plain))
                .expiresAt(req.expiresAt())
                .build();
        personalTokenRepository.save(token);
        return new PersonalTokenCreated(token.getId(), token.getName(), plain, token.getCreatedAt(), token.getExpiresAt());
    }

    public void deleteToken(UUID tokenId) {
        User user = currentUserService.getCurrentUser();
        PersonalToken token = personalTokenRepository.findById(tokenId)
                .orElseThrow(() -> new NotFoundException("Token introuvable"));
        if (!token.getUser().getId().equals(user.getId()))
            throw new ForbiddenException("Ce token ne vous appartient pas");
        personalTokenRepository.delete(token);
    }

    @Transactional
    public Optional<Authentication> validateToken(String plainToken) {
        return personalTokenRepository.findByTokenHash(sha256(plainToken))
                .filter(t -> !t.isExpired())
                .map(t -> {
                    t.setLastUsedAt(LocalDateTime.now());
                    User user = t.getUser();
                    return (Authentication) new UsernamePasswordAuthenticationToken(
                            user.getEmail(), null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
                });
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return "hm_" + HexFormat.of().formatHex(bytes);
    }

    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private PersonalTokenResponse toResponse(PersonalToken t) {
        return new PersonalTokenResponse(t.getId(), t.getName(), t.getCreatedAt(), t.getLastUsedAt(), t.getExpiresAt());
    }
}
