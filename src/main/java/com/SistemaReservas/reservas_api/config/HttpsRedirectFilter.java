package com.SistemaReservas.reservas_api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Redireciona HTTP -> HTTPS quando a aplicação está exposta diretamente (sem um
 * proxy reverso cuidando disso). Só ativa com "security.require-https=true", pensado
 * para produção. "request.isSecure()" já reflete corretamente X-Forwarded-Proto
 * graças a "server.forward-headers-strategy=framework" quando há um proxy na frente.
 *
 * IMPORTANTE: na prática, a forma mais comum e robusta de forçar HTTPS é terminar TLS
 * no proxy reverso (nginx/load balancer) e redirecionar ali - este filtro é um reforço
 * para quando a aplicação roda sem proxy na frente. Ver SECURITY.md.
 */
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
