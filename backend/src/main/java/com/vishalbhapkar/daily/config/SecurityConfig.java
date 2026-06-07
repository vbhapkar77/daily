package com.vishalbhapkar.daily.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * TEMPORARY permissive config so the empty scaffold app responds while we work on
 * Phase 2 verification. Real auth is implemented in spec 001-auth-signup-and-login
 * (per ADR-0001). This bean will be REPLACED at that time — the JWT filter chain,
 * rate limiter, and all auth rules go in then.
 *
 * <p>What this currently does:
 * <ul>
 *   <li>Permits all requests (no auth required)
 *   <li>Disables CSRF (we're stateless; will be re-evaluated with the real auth design)
 *   <li>Disables HTTP Basic + form login defaults
 *   <li>Enables CORS for origins listed in {@code app.cors.allowed-origins}
 * </ul>
 */
@Configuration
public class SecurityConfig {

    /**
     * Comma-separated list of allowed origins. Driven by env var so we don't hardcode
     * the production Vercel URL. Default covers local dev.
     */
    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(origins);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
