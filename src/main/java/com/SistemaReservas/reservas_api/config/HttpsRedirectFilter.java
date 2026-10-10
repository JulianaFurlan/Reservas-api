package com.SistemaReservas.reservas_api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@ConditionalOnProperty(name = "security.require-https", havingValue = "true")
public class HttpsRedirectFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!request.isSecure()) {
            String url = "https://" + request.getServerName() + request.getRequestURI();
            String query = request.getQueryString();
            if (query != null) {
                url += "?" + query;
            }
            response.sendRedirect(url);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
