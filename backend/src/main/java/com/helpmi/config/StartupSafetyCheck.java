package com.helpmi.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class StartupSafetyCheck {

    @Value("${app.security.disabled:false}")
    private boolean securityDisabled;

    private final Environment environment;

    public StartupSafetyCheck(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validate() {
        if (securityDisabled && Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            throw new IllegalStateException(
                    "SECURITY ERROR: app.security.disabled=true must never be used with the 'prod' profile. " +
                    "Remove it from configuration or do not activate the prod profile in dev mode.");
        }
    }
}
