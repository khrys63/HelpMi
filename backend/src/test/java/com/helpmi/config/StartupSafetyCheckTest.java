package com.helpmi.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StartupSafetyCheckTest {

    private StartupSafetyCheck check(boolean securityDisabled, String... activeProfiles) {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles(activeProfiles);
        StartupSafetyCheck check = new StartupSafetyCheck(env);
        // Inject the boolean field via reflection since it's set via @Value in prod
        try {
            var field = StartupSafetyCheck.class.getDeclaredField("securityDisabled");
            field.setAccessible(true);
            field.set(check, securityDisabled);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return check;
    }

    @Test
    void securityEnabled_prodProfile_passes() {
        assertThatCode(() -> check(false, "prod").validate())
                .doesNotThrowAnyException();
    }

    @Test
    void securityDisabled_devProfile_passes() {
        assertThatCode(() -> check(true, "dev").validate())
                .doesNotThrowAnyException();
    }

    @Test
    void securityDisabled_noProfile_passes() {
        assertThatCode(() -> check(true).validate())
                .doesNotThrowAnyException();
    }

    @Test
    void securityDisabled_prodProfile_throwsIllegalState() {
        assertThatThrownBy(() -> check(true, "prod").validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("prod");
    }

    @Test
    void securityEnabled_prodAndDevProfiles_passes() {
        // only "prod" alone triggers the guard
        assertThatCode(() -> check(false, "prod", "dev").validate())
                .doesNotThrowAnyException();
    }
}
