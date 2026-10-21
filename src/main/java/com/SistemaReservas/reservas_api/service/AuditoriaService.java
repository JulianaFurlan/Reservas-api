package com.SistemaReservas.reservas_api.service;

import com.SistemaReservas.reservas_api.model.RegistroAuditoria;
import com.SistemaReservas.reservas_api.repository.RegistroAuditoriaRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

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
