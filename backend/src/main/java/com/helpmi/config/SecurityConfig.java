package com.helpmi.config;

import com.helpmi.security.DevAuthFilter;
import com.helpmi.security.PersonalTokenFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.security.disabled:false}")
    private boolean securityDisabled;

    @Autowired(required = false)
    private DevAuthFilter devAuthFilter;

    @Autowired(required = false)
    private PersonalTokenFilter personalTokenFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults());

        if (securityDisabled) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            if (devAuthFilter != null) {
                http.addFilterBefore(devAuthFilter, UsernamePasswordAuthenticationFilter.class);
            }
        } else {
            if (personalTokenFilter != null) {
                http.addFilterBefore(personalTokenFilter, BearerTokenAuthenticationFilter.class);
            }
            http.authorizeHttpRequests(auth -> auth
                            .requestMatchers("/actuator/health").permitAll()
                            .anyRequest().authenticated())
                    .oauth2ResourceServer(oauth2 -> oauth2
                            .bearerTokenResolver(new JwtOnlyBearerTokenResolver())
                            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        }

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter((Jwt jwt) -> {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess == null) return Collections.emptyList();
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get("roles");
            if (roles == null) return Collections.emptyList();
            return roles.stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .map(a -> (org.springframework.security.core.GrantedAuthority) a)
                    .toList();
        });
        return converter;
    }

    // Passes the Bearer token to the OAuth2 filter only if it looks like a JWT (has 2 dots).
    // PATs are handled upstream by PersonalTokenFilter.
    private static class JwtOnlyBearerTokenResolver implements BearerTokenResolver {
        private final DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();

        @Override
        public String resolve(HttpServletRequest request) {
            String token = delegate.resolve(request);
            if (token == null) return null;
            return token.chars().filter(c -> c == '.').count() == 2 ? token : null;
        }
    }
}
