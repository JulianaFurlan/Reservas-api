package com.SistemaReservas.reservas_api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Remove o header "Server" (quando presente) para não revelar a tecnologia/versão do
 * servidor de aplicação às respostas HTTP.
 */
@Component
public class ServerHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        response.setHeader("Server", "");
        filterChain.doFilter(request, response);
    }
}
