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

        boolean temConflito = repository.findAll().stream().anyMatch(existing -> {

            // Validação de null
            if (existing.getSala() == null || reserva.getSala() == null ||
                existing.getData() == null || reserva.getData() == null) {
                return false;
            }

            // Mesma sala e mesma data
            if (!existing.getSala().equals(reserva.getSala()) ||
                !existing.getData().equals(reserva.getData())) {
                return false;
            }

            // Evita conflito com ela mesma na edição
            if (reserva.getId() != null && existing.getId().equals(reserva.getId())) {
                return false;
            }

            // Verifica sobreposição de horários
            boolean sobrepoe = !(
                reserva.getHoraFim().isBefore(existing.getHoraInicio()) ||
                reserva.getHoraInicio().isAfter(existing.getHoraFim())
            );

            return sobrepoe;
        });

        if (temConflito) {
            throw new RuntimeException("Esta sala já está reservada neste dia e período.");
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
}