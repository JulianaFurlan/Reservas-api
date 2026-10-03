package com.SistemaReservas.reservas_api.controller;

import com.SistemaReservas.reservas_api.model.RegistroAuditoria;
import com.SistemaReservas.reservas_api.repository.RegistroAuditoriaRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/auditoria")
@PreAuthorize("hasRole('ADMIN')")
public class AuditoriaController {

    private final RegistroAuditoriaRepository repository;

    public AuditoriaController(RegistroAuditoriaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<RegistroAuditoria> listar() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(RegistroAuditoria::getDataHora).reversed())
                .toList();
    }
}
