package com.SistemaReservas.reservas_api.service;

import com.SistemaReservas.reservas_api.model.Reserva;
import com.SistemaReservas.reservas_api.model.Sala;
import com.SistemaReservas.reservas_api.repository.ReservaRepository;
import com.SistemaReservas.reservas_api.repository.SalaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReservaService {

    private final ReservaRepository repository;
    private final SalaRepository salaRepository;
    private final EmailService emailService;

    public ReservaService(ReservaRepository repository,
                          SalaRepository salaRepository,
                          EmailService emailService) {  // ← adicionado aqui
        this.repository = repository;
        this.salaRepository = salaRepository;
        this.emailService = emailService;  // ← adicionado aqui
    }

    public List<Reserva> listarTodas() {
        return repository.findAll();
    }

    public List<Reserva> listarAprovadas() {
        return repository.findAll().stream()
                .filter(r -> "APROVADO".equals(r.getStatus()))
                .toList();
    }

    public Reserva salvar(Reserva reserva) {
        if (reserva.getSalaId() != null) {
            Sala sala = salaRepository.findById(reserva.getSalaId())
                    .orElseThrow(() -> new RuntimeException("Sala não encontrada"));
            reserva.setSalaNome(sala.getNome());
        }

        boolean temConflito = repository.findAll().stream().anyMatch(existing -> {
            if (existing.getSalaId() == null || reserva.getSalaId() == null
                    || existing.getData() == null || reserva.getData() == null) {
                return false;
            }
            if (!existing.getSalaId().equals(reserva.getSalaId())
                    || !existing.getData().equals(reserva.getData())) {
                return false;
            }
            if (reserva.getId() != null && existing.getId().equals(reserva.getId())) {
                return false;
            }
            if (!"APROVADO".equals(existing.getStatus())) {
                return false;
            }
            return !(reserva.getHoraFim().isBefore(existing.getHoraInicio())
                    || reserva.getHoraInicio().isAfter(existing.getHoraFim()));
        });

        if (temConflito) {
            throw new RuntimeException("Esta sala já possui uma reserva aprovada neste horário.");
        }

        if (reserva.getStatus() == null) {
            reserva.setStatus("PENDENTE");
        }

        Reserva salva = repository.save(reserva);
        emailService.notificarReservaCriada(salva); // ← notifica criação
        return salva;
    }

    public Reserva buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva não encontrada"));
    }

    public Reserva aprovar(Long id) {
        Reserva reserva = buscarPorId(id);
        reserva.setStatus("APROVADO");
        Reserva salva = repository.save(reserva);
        emailService.notificarReservaAprovada(salva); // ← notifica aprovação
        return salva;
    }

    public Reserva rejeitar(Long id, String motivo) {
        Reserva reserva = buscarPorId(id);
        reserva.setStatus("REJEITADO");
        reserva.setMotivoRejeicao(motivo);
        Reserva salva = repository.save(reserva);
        emailService.notificarReservaRejeitada(salva); // ← notifica rejeição
        return salva;
    }

    public List<Reserva> listarHistorico() {
        return repository.findAll().stream()
                .filter(r -> "APROVADO".equals(r.getStatus()) || "REJEITADO".equals(r.getStatus()))
                .sorted((a, b) -> b.getData().compareTo(a.getData()))
                .toList();
    }

    public Reserva reverter(Long id) {
        Reserva reserva = buscarPorId(id);
        if (reserva.getData().isBefore(java.time.LocalDate.now())) {
            throw new RuntimeException("Não é possível reverter uma reserva com data já passada.");
        }
        reserva.setStatus("PENDENTE");
        reserva.setMotivoRejeicao(null);
        return repository.save(reserva);
    }

    public Reserva cancelar(Long id) {
        Reserva reserva = buscarPorId(id);
        reserva.setStatus("CANCELADO");
        return repository.save(reserva);
    }

    public List<Reserva> listarPorUsuario(Long usuarioId) {
        return repository.findByUsuarioId(usuarioId);
    }
}