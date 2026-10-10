package com.SistemaReservas.reservas_api.config;

import com.SistemaReservas.reservas_api.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) ->
                                escreverErro(response, HttpServletResponse.SC_UNAUTHORIZED,
                                        "Sessão expirada ou token inválido. Faça login novamente."))
                        .accessDeniedHandler((request, response, e) ->
                                escreverErro(response, HttpServletResponse.SC_FORBIDDEN, "Acesso negado"))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()

                        // somente admin
                        .requestMatchers("/api/auditoria/**").hasRole("ADMIN")

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
                        // atualizar/cancelar: autenticado aqui
                        .requestMatchers(HttpMethod.PUT, "/api/reservas/**").authenticated()

                        .requestMatchers(HttpMethod.GET, "/api/salas/**").authenticated()
                        .requestMatchers("/api/salas/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void escreverErro(HttpServletResponse response, int status, String mensagem) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"message\":\"" + mensagem + "\"}");
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
        config.setAllowedHeaders(List.of("Content-Type"));
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
