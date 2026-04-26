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
import com.helpmi.service.RateLimiterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.helpmi.Fixtures.adminUser;
import static com.helpmi.Fixtures.agentUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonalTokenServiceTest {

    @Mock PersonalTokenRepository personalTokenRepository;
    @Mock CurrentUserService currentUserService;
    @Mock RateLimiterService rateLimiterService;

    @InjectMocks PersonalTokenService service;

    // ── listTokens ────────────────────────────────────────────────────────────

    @Test
    void listTokens_returnsTokensForCurrentUser() {
        User user = adminUser();
        PersonalToken token = tokenFor(user);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(personalTokenRepository.findByUserIdOrderByCreatedAtDesc(user.getId()))
                .thenReturn(List.of(token));

        List<PersonalTokenResponse> result = service.listTokens();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("My Token");
    }

    // ── createToken ───────────────────────────────────────────────────────────

    @Test
    void createToken_returnsPlainTokenNotHash() {
        User user = adminUser();
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(personalTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PersonalTokenCreated result = service.createToken(new CreatePersonalTokenRequest("CI Token", null));

        assertThat(result.plainToken()).startsWith("hm_");
        assertThat(result.plainToken()).hasSize(67); // "hm_" + 64 hex chars
    }

    @Test
    void createToken_hashIsNotPlainToken() {
        User user = adminUser();
        when(currentUserService.getCurrentUser()).thenReturn(user);
        ArgumentCaptor<PersonalToken> captor = ArgumentCaptor.forClass(PersonalToken.class);
        when(personalTokenRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        PersonalTokenCreated result = service.createToken(new CreatePersonalTokenRequest("CI Token", null));

        String storedHash = captor.getValue().getTokenHash();
        assertThat(storedHash).isNotEqualTo(result.plainToken());
        assertThat(storedHash).isEqualTo(PersonalTokenService.sha256(result.plainToken()));
    }

    @Test
    void createToken_withExpiry_setsExpiresAt() {
        User user = adminUser();
        LocalDateTime expiry = LocalDateTime.now().plusDays(30);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        ArgumentCaptor<PersonalToken> captor = ArgumentCaptor.forClass(PersonalToken.class);
        when(personalTokenRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        service.createToken(new CreatePersonalTokenRequest("Temp Token", expiry));

        assertThat(captor.getValue().getExpiresAt()).isEqualTo(expiry);
    }

    @Test
    void createToken_twoCallsGenerateDifferentTokens() {
        User user = adminUser();
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(personalTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String token1 = service.createToken(new CreatePersonalTokenRequest("A", null)).plainToken();
        String token2 = service.createToken(new CreatePersonalTokenRequest("B", null)).plainToken();

        assertThat(token1).isNotEqualTo(token2);
    }

    // ── deleteToken ───────────────────────────────────────────────────────────

    @Test
    void deleteToken_ownToken_deletesSuccessfully() {
        User user = adminUser();
        PersonalToken token = tokenFor(user);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(personalTokenRepository.findById(token.getId())).thenReturn(Optional.of(token));

        service.deleteToken(token.getId());

        verify(personalTokenRepository).delete(token);
    }

    @Test
    void deleteToken_notFound_throwsNotFoundException() {
        UUID id = UUID.randomUUID();
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());
        when(personalTokenRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteToken(id))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteToken_otherUsersToken_throwsForbidden() {
        User owner = adminUser();
        User other = agentUser();
        PersonalToken token = tokenFor(owner);
        when(currentUserService.getCurrentUser()).thenReturn(other);
        when(personalTokenRepository.findById(token.getId())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.deleteToken(token.getId()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("appartient pas");
    }

    // ── validateToken ─────────────────────────────────────────────────────────

    @Test
    void validateToken_validToken_returnsAuthentication() {
        User user = adminUser();
        String plain = "hm_abc123";
        String hash = PersonalTokenService.sha256(plain);
        PersonalToken token = tokenFor(user);
        token.setTokenHash(hash);
        when(personalTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(token));

        Optional<Authentication> result = service.validateToken(plain);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo(user.getEmail());
    }

    @Test
    void validateToken_validToken_setsRoleAuthority() {
        User user = adminUser();
        String plain = "hm_abc";
        PersonalToken token = tokenFor(user);
        token.setTokenHash(PersonalTokenService.sha256(plain));
        when(personalTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        Authentication auth = service.validateToken(plain).orElseThrow();

        assertThat(auth.getAuthorities()).extracting(Object::toString).contains("ROLE_ADMIN");
    }

    @Test
    void validateToken_validToken_updatesLastUsedAt() {
        User user = adminUser();
        String plain = "hm_abc";
        PersonalToken token = tokenFor(user);
        token.setTokenHash(PersonalTokenService.sha256(plain));
        assertThat(token.getLastUsedAt()).isNull();
        when(personalTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        service.validateToken(plain);

        assertThat(token.getLastUsedAt()).isNotNull();
    }

    @Test
    void validateToken_expiredToken_returnsEmpty() {
        User user = adminUser();
        String plain = "hm_abc";
        PersonalToken token = tokenFor(user);
        token.setTokenHash(PersonalTokenService.sha256(plain));
        token.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(personalTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        Optional<Authentication> result = service.validateToken(plain);

        assertThat(result).isEmpty();
    }

    @Test
    void validateToken_unknownHash_returnsEmpty() {
        when(personalTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        Optional<Authentication> result = service.validateToken("hm_unknown");

        assertThat(result).isEmpty();
    }

    @Test
    void validateToken_notYetExpiredToken_returnsAuthentication() {
        User user = adminUser();
        String plain = "hm_abc";
        PersonalToken token = tokenFor(user);
        token.setTokenHash(PersonalTokenService.sha256(plain));
        token.setExpiresAt(LocalDateTime.now().plusDays(1));
        when(personalTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        assertThat(service.validateToken(plain)).isPresent();
    }

    // ── sha256 ────────────────────────────────────────────────────────────────

    @Test
    void sha256_isDeterministic() {
        assertThat(PersonalTokenService.sha256("hello"))
                .isEqualTo(PersonalTokenService.sha256("hello"));
    }

    @Test
    void sha256_differentInputsProduceDifferentHashes() {
        assertThat(PersonalTokenService.sha256("hello"))
                .isNotEqualTo(PersonalTokenService.sha256("world"));
    }

    @Test
    void sha256_outputIs64HexChars() {
        assertThat(PersonalTokenService.sha256("any input")).hasSize(64);
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private PersonalToken tokenFor(User user) {
        return PersonalToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .name("My Token")
                .tokenHash("dummy_hash")
                .build();
    }
}
