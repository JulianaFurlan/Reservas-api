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

    public Reserva salvar(Reserva reserva) {
        // Se for uma reserva existente (edição), não valida conflito com ela mesma
        boolean temConflito = repository.findAll().stream().anyMatch(existing -> {
            if (existing.getSala() == null || reserva.getSala() == null ||
                    existing.getData() == null || reserva.getData() == null) {
                return false;
            }

            if (!existing.getSala().equals(reserva.getSala()) ||
                    !existing.getData().equals(reserva.getData())) {
                return false;
            }

            // Ignora a própria reserva na edição
            if (reserva.getId() != null && existing.getId().equals(reserva.getId())) {
                return false;
            }

            return !(reserva.getHoraFim().isBefore(existing.getHoraInicio()) ||
                    reserva.getHoraInicio().isAfter(existing.getHoraFim()));
        });

        if (temConflito) {
            throw new RuntimeException("Esta sala já está reservada neste dia e período.");
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

    public Reserva rejeitar(Long id) {
        Reserva reserva = buscarPorId(id);
        reserva.setStatus("REJEITADO");
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