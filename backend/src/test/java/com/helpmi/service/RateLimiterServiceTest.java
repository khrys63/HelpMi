package com.helpmi.service;

import com.helpmi.exception.TooManyRequestsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimiterServiceTest {

    private RateLimiterService service;

    @BeforeEach
    void setUp() {
        service = new RateLimiterService();
    }

    @Test
    void firstRequest_doesNotThrow() {
        assertThatCode(() -> service.checkTokenCreation(UUID.randomUUID()))
                .doesNotThrowAnyException();
    }

    @Test
    void underLimit_doesNotThrow() {
        UUID userId = UUID.randomUUID();
        for (int i = 0; i < 9; i++) {
            service.checkTokenCreation(userId);
        }
        // 9th call should still succeed
        assertThatCode(() -> service.checkTokenCreation(userId))
                .doesNotThrowAnyException();
    }

    @Test
    void atLimit_throwsTooManyRequests() {
        UUID userId = UUID.randomUUID();
        for (int i = 0; i < 10; i++) {
            service.checkTokenCreation(userId);
        }
        assertThatThrownBy(() -> service.checkTokenCreation(userId))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessageContaining("10");
    }

    @Test
    void differentUsers_haveIndependentCounters() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        for (int i = 0; i < 10; i++) {
            service.checkTokenCreation(userA);
        }

        // userB has no requests yet — should not be affected by userA's limit
        assertThatCode(() -> service.checkTokenCreation(userB))
                .doesNotThrowAnyException();
    }

    @Test
    void exceeding_limit_messageContainsHourInfo() {
        UUID userId = UUID.randomUUID();
        for (int i = 0; i < 10; i++) {
            service.checkTokenCreation(userId);
        }
        assertThatThrownBy(() -> service.checkTokenCreation(userId))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessageContaining("heure");
    }
}
