package com.SistemaReservas.reservas_api.service;

import com.SistemaReservas.reservas_api.model.Reserva;
import com.SistemaReservas.reservas_api.model.Sala;
import com.SistemaReservas.reservas_api.model.enums.StatusSala;
import com.SistemaReservas.reservas_api.repository.ReservaRepository;
import com.SistemaReservas.reservas_api.repository.SalaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
public class SalaService {

    private final SalaRepository repository;
    private final ReservaRepository reservaRepository;
    private final EmailService emailService;

    public SalaService(SalaRepository repository, ReservaRepository reservaRepository, EmailService emailService) {
        this.repository = repository;
        this.reservaRepository = reservaRepository;
        this.emailService = emailService;
    }

    public List<Sala> listarTodas() {
        return repository.findAll();
    }

    public List<Sala> listarDisponiveis() {
        return repository.findByStatus(StatusSala.ATIVA.name());
    }

    public Sala buscarPorId(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Sala não encontrada"));
    }

    public Sala salvar(Sala sala) {
        if (sala.getNome() == null || sala.getNome().trim().isEmpty()) {
            throw new RuntimeException("O nome da sala é obrigatório.");
        }
        if (sala.getBloco() == null || sala.getBloco().trim().isEmpty()) {
            throw new RuntimeException("O bloco da sala é obrigatório.");
        }
        if (sala.getCapacidade() != null && sala.getCapacidade() <= 0) {
            throw new RuntimeException("Capacidade deve ser maior que zero");
        }
        if (!statusValido(sala.getStatus())) {
            throw new RuntimeException("Status inválido");
        }
        // Valida duplicata apenas no cadastro
        if (sala.getId() == null && repository.existsByNomeAndBloco(sala.getNome(), sala.getBloco())) {
            throw new RuntimeException("Já existe uma sala com este nome neste bloco");
        }
        return repository.save(sala);
    }

    private boolean statusValido(String status) {
        return Arrays.stream(StatusSala.values()).anyMatch(s -> s.name().equals(status));
    }

    public Sala alterarStatus(Long id, String status) {
        Sala sala = buscarPorId(id);

        if (!status.equals("ATIVA")) {
            List<Reserva> reservasFuturas = reservaRepository
                    .findBySalaIdAndStatusInAndDataGreaterThanEqual(
                            id,
                            List.of("PENDENTE", "APROVADO", "APROVADA"),
                            LocalDate.now()
                    );

            reservasFuturas.forEach(r -> {
                r.setStatus("CANCELADO");
                r.setMotivoRejeicao("Sala indisponível — alteração administrativa");
                reservaRepository.save(r);
                emailService.notificarCancelamentoAdministrativo(r, status);
            });
        }

        sala.setStatus(status);
        return repository.save(sala);
    }

    public void deletar(Long id) {
        Sala sala = buscarPorId(id);

        //Verifica se tem reservas vinculadas
        boolean temReservas = reservaRepository.existsBySalaId(sala.getId());
        if (temReservas) {
            throw new RuntimeException("Esta sala possui reservas no histórico e não pode ser excluída. " + "Coloque-a em inativa para impedir novas reservas.");
        }

        repository.deleteById(id);
    }
}