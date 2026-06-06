package com.SistemaReservas.reservas_api.service;

import com.SistemaReservas.reservas_api.model.Reserva;
import com.SistemaReservas.reservas_api.repository.ReservaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReservaService {

    private final ReservaRepository repository;

    public ReservaService(ReservaRepository repository) {
        this.repository = repository;
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
        boolean temConflito = repository.findAll().stream().anyMatch(existing -> {
            if (existing.getSala() == null || reserva.getSala() == null ||
                    existing.getData() == null || reserva.getData() == null) {
                return false;
            }

            if (!existing.getSala().equals(reserva.getSala()) ||
                    !existing.getData().equals(reserva.getData())) {
                return false;
            }

            if (reserva.getId() != null && existing.getId().equals(reserva.getId())) {
                return false;
            }

            if (!"APROVADO".equals(existing.getStatus())) {
                return false;
            }

            return !(reserva.getHoraFim().isBefore(existing.getHoraInicio()) ||
                    reserva.getHoraInicio().isAfter(existing.getHoraFim()));
        });

        if (temConflito) {
            throw new RuntimeException("Esta sala já possui uma reserva aprovada neste horário.");
        }

        if (reserva.getStatus() == null) {
            reserva.setStatus("PENDENTE");
        }

        return repository.save(reserva);
    }
    public void deletar(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Reserva não encontrada");
        }
        repository.deleteById(id);
    }

    public Reserva buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva não encontrada"));
    }

    public Reserva aprovar(Long id) {
        Reserva reserva = buscarPorId(id);
        reserva.setStatus("APROVADO");
        return repository.save(reserva);
    }

    public Reserva rejeitar(Long id, String motivo) {
        Reserva reserva = buscarPorId(id);
        reserva.setStatus("REJEITADO");
        reserva.setMotivoRejeicao(motivo);
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