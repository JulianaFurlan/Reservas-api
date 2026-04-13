package com.SistemaReservas.reservas_api.service;

import com.SistemaReservas.reservas_api.model.Reserva;
import com.SistemaReservas.reservas_api.repository.ReservaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReservaService {

    @Autowired
    private ReservaRepository repository;

    public List<Reserva> listarTodas() {
        return repository.findAll();
    }

    public Reserva salvar (Reserva reserva) {
        //Validação de conflito de horários
        boolean temConflito = repository.findAll().stream().anyMatch(existing -> {

            //Mesma Sala e mesma data/horário
            if(!existing.getSala().equals(reserva.getSala()) || !existing.getData().equals(reserva.getData())) {
                return false;
            }

            //Na edição, não cria conflito com a própria reserva
            if (reserva.getId() != null && existing.getId().equals(reserva.getId())) {
                return false;
            }

            //Verifica se os horários se sobrepõem
            boolean sobrepoe = !(reserva.getHoraFim().isBefore(existing.getHoraInicio()) || reserva.getHoraInicio().isAfter(existing.getHoraFim()));
            return sobrepoe;
        });

        if (temConflito) {
            throw new RuntimeException("Conflito de horário! Está sala já esta reservada neste dia e período.");
        }

        return repository.save(reserva);
    }

    public void deletar(Long id) {
        repository.deleteById(id);
    }

    public Reserva buscarPorId(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Reserva não encontrada"));    }
}
