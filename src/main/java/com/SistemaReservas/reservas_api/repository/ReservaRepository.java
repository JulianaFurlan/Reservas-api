package com.SistemaReservas.reservas_api.repository;

import com.SistemaReservas.reservas_api.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    List<Reserva> findByUsuarioId(Long usuarioId);
    boolean existsBySalaId(Long salaId);
    boolean existsByUsuarioId(Long usuarioId);

    List<Reserva> findBySalaIdAndStatusInAndDataAfter(
            Long salaId,
            List<String> statuses,
            LocalDate data
    );
}
