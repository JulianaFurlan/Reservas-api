package com.SistemaReservas.reservas_api.config;

import com.SistemaReservas.reservas_api.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(contentType -> {})
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                        )
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                        )
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; frame-ancestors 'none'")
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()

                        // usuario específico
                        .requestMatchers("/api/usuarios/perfil/**").authenticated()

                        // usuarios gerais (gestao administrativa de contas)
                        .requestMatchers("/api/usuarios/**").hasRole("ADMIN")

                        // decisões sobre reservas: somente gestor/admin
                        .requestMatchers(HttpMethod.PUT, "/api/reservas/*/aprovar").hasAnyRole("GESTOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/reservas/*/rejeitar").hasAnyRole("GESTOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/reservas/*/reverter").hasAnyRole("GESTOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/reservas/*/contatar").hasAnyRole("GESTOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/reservas/historico").hasAnyRole("GESTOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/reservas/todas").hasAnyRole("GESTOR", "ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/reservas/aprovadas").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/reservas").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/reservas/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/reservas").authenticated()
                        // atualizar/cancelar: autenticado aqui; posse (dono ou gestor/admin) é checada no controller/service
                        .requestMatchers(HttpMethod.PUT, "/api/reservas/**").authenticated()

                        .requestMatchers(HttpMethod.GET, "/api/salas/**").authenticated()
                        .requestMatchers("/api/salas/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
