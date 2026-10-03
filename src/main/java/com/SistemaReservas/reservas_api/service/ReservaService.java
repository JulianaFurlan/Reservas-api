package com.SistemaReservas.reservas_api.service;

import com.SistemaReservas.reservas_api.dto.request.ReservaRequest;
import com.SistemaReservas.reservas_api.model.Reserva;
import com.SistemaReservas.reservas_api.model.Sala;
import com.SistemaReservas.reservas_api.model.Usuario;
import com.SistemaReservas.reservas_api.model.enums.TipoUsuario;
import com.SistemaReservas.reservas_api.repository.ReservaRepository;
import com.SistemaReservas.reservas_api.repository.SalaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservaService {

    private static final int ANTECEDENCIA_MINIMA_MINUTOS = 10;
    private static final int ANTECEDENCIA_MAXIMA_ANOS = 1;

    private final ReservaRepository repository;
    private final SalaRepository salaRepository;
    private final EmailService emailService;

    public ReservaService(ReservaRepository repository,
                          SalaRepository salaRepository,
                          EmailService emailService) {
        this.repository = repository;
        this.salaRepository = salaRepository;
        this.emailService = emailService;
    }

    public List<Reserva> listarTodas() {
        return repository.findAll();
    }

    public List<Reserva> listarAprovadas() {
        return repository.findAll().stream()
                .filter(r -> "APROVADO".equals(r.getStatus()))
                .toList();
    }

    public boolean podeGerenciar(Usuario usuario, Reserva reserva) {
        if (usuario.getTipo() == TipoUsuario.ADMIN || usuario.getTipo() == TipoUsuario.GESTOR) {
            return true;
        }
        return usuario.getId().equals(reserva.getUsuarioId());
    }

    public Reserva criar(ReservaRequest request, Usuario usuarioLogado) {
        Reserva reserva = new Reserva();
        aplicarCamposEditaveis(reserva, request);

        reserva.setUsuarioId(usuarioLogado.getId());
        reserva.setUsuarioEmail(usuarioLogado.getEmail());
        reserva.setUsuarioNome(usuarioLogado.getNome());
        reserva.setStatus("PENDENTE");

        return salvarComValidacoes(reserva);
    }

    public Reserva atualizar(Long id, ReservaRequest request, Usuario usuarioLogado) {
        Reserva reserva = buscarPorId(id);
        if (!podeGerenciar(usuarioLogado, reserva)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Você não tem permissão para editar esta reserva");
        }

        // status, usuarioId, usuarioNome, usuarioEmail e motivoRejeicao nunca vêm do
        // corpo da requisição: são controlados só pelo backend (evita mass assignment,
        // ex: um usuário comum definir status=APROVADO na própria reserva).
        aplicarCamposEditaveis(reserva, request);

        return salvarComValidacoes(reserva);
    }

    private void aplicarCamposEditaveis(Reserva reserva, ReservaRequest request) {
        if (request.getSalaId() != null) {
            Sala sala = salaRepository.findById(request.getSalaId())
                    .orElseThrow(() -> new RuntimeException("Sala não encontrada"));
            reserva.setSalaId(sala.getId());
            reserva.setSalaNome(sala.getNome());
        }
        reserva.setData(request.getData());
        reserva.setHoraInicio(request.getHoraInicio());
        reserva.setHoraFim(request.getHoraFim());
        reserva.setNome(request.getNome());
        reserva.setEmail(request.getEmail());
        reserva.setTelefone(request.getTelefone());
        reserva.setDepartamento(request.getDepartamento());
        reserva.setFinalidade(request.getFinalidade());
        reserva.setObservacoes(request.getObservacoes());
    }

    private Reserva salvarComValidacoes(Reserva reserva) {
        validarAntecedencia(reserva);
        validarConflito(reserva);

        Reserva salva = repository.save(reserva);
        emailService.notificarReservaCriada(salva);
        return salva;
    }

    private void validarAntecedencia(Reserva reserva) {
        if (reserva.getData() == null || reserva.getHoraInicio() == null) {
            return;
        }

        LocalDateTime inicio = LocalDateTime.of(reserva.getData(), reserva.getHoraInicio());

        if (inicio.isBefore(LocalDateTime.now().plusMinutes(ANTECEDENCIA_MINIMA_MINUTOS))) {
            throw new RuntimeException(
                    "A reserva deve ser feita com pelo menos " + ANTECEDENCIA_MINIMA_MINUTOS + " minutos de antecedência.");
        }

        LocalDate limiteMaximo = LocalDate.now().plusYears(ANTECEDENCIA_MAXIMA_ANOS);
        if (reserva.getData().isAfter(limiteMaximo)) {
            throw new RuntimeException(
                    "A reserva não pode ser feita com mais de " + ANTECEDENCIA_MAXIMA_ANOS + " ano(s) de antecedência.");
        }
    }

    private void validarConflito(Reserva reserva) {
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
    }

    public Reserva buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva não encontrada"));
    }

    public Reserva aprovar(Long id) {
        Reserva reserva = buscarPorId(id);
        reserva.setStatus("APROVADO");
        Reserva salva = repository.save(reserva);
        emailService.notificarReservaAprovada(salva);
        return salva;
    }

    public Reserva rejeitar(Long id, String motivo) {
        Reserva reserva = buscarPorId(id);
        reserva.setStatus("REJEITADO");
        reserva.setMotivoRejeicao(motivo);
        Reserva salva = repository.save(reserva);
        emailService.notificarReservaRejeitada(salva);
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

    public Reserva cancelar(Long id, Usuario usuarioLogado) {
        Reserva reserva = buscarPorId(id);
        if (!podeGerenciar(usuarioLogado, reserva)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Você não tem permissão para cancelar esta reserva");
        }
        reserva.setStatus("CANCELADO");
        return repository.save(reserva);
    }

    public List<Reserva> listarPorUsuario(Long usuarioId) {
        return repository.findByUsuarioId(usuarioId);
    }
}
