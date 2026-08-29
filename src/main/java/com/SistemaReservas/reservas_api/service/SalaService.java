package com.SistemaReservas.reservas_api.service;

import com.SistemaReservas.reservas_api.model.Reserva;
import com.SistemaReservas.reservas_api.model.Sala;
import com.SistemaReservas.reservas_api.repository.ReservaRepository;
import com.SistemaReservas.reservas_api.repository.SalaRepository;
import jakarta.validation.constraints.Email;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
        return repository.findByStatus("ATIVO");
    }

    public Sala buscarPorId(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Sala não encontrada"));
    }

    public Sala salvar(Sala sala) {
        // Valida duplicata apenas no cadastro
        if (sala.getId() == null && repository.existsByNomeAndBloco(sala.getNome(), sala.getBloco())) {
            throw new RuntimeException("Já existe uma sala com este nome neste bloco");
        }
        return repository.save(sala);
    }

    public Sala alterarStatus(Long id, String status) {
        Sala sala = buscarPorId(id);

        if (!status.equals("ATIVA")) {
            List<Reserva> reservasFuturas = reservaRepository
                    .findBySalaIdAndStatusInAndDataAfter(
                            id,
                            List.of("PENDENTE", "APROVADO"),
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