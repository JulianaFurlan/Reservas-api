package com.SistemaReservas.reservas_api.repository;

import com.SistemaReservas.reservas_api.model.Sala;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalaRepository extends JpaRepository<Sala, Long> {
    boolean existsByNomeAndBloco(String nome, String bloco);
    List<Sala> findByStatus(String status);
}