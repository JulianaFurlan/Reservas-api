package com.SistemaReservas.reservas_api.service;

import com.SistemaReservas.reservas_api.model.RegistroAuditoria;
import com.SistemaReservas.reservas_api.repository.RegistroAuditoriaRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Registra ações administrativas sensíveis (aprovação/rejeição de reservas, reset de
 * senha, ativação/desativação de contas) para rastreabilidade, já que o sistema não
 * tem outra forma de auditoria. Resolve automaticamente o usuário autenticado que
 * disparou a ação a partir do SecurityContext.
 */
@Service
public class AuditoriaService {

    private final RegistroAuditoriaRepository repository;

    public AuditoriaService(RegistroAuditoriaRepository repository) {
        this.repository = repository;
    }

    public void registrar(String acao, String detalhe) {
        String emailAutor = emailDoUsuarioAtual();
        repository.save(new RegistroAuditoria(emailAutor, acao, detalhe));
    }

    private String emailDoUsuarioAtual() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return "desconhecido";
    }
}
