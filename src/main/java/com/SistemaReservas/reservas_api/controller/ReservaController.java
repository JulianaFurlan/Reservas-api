package com.SistemaReservas.reservas_api.controller;

import com.SistemaReservas.reservas_api.dto.request.ReservaRequest;
import com.SistemaReservas.reservas_api.model.Reserva;
import com.SistemaReservas.reservas_api.model.Usuario;
import com.SistemaReservas.reservas_api.repository.UsuarioRepository;
import com.SistemaReservas.reservas_api.service.AuditoriaService;
import com.SistemaReservas.reservas_api.service.EmailService;
import com.SistemaReservas.reservas_api.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService service;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final AuditoriaService auditoriaService;

    public ReservaController(ReservaService service, UsuarioRepository usuarioRepository,
                             EmailService emailService, AuditoriaService auditoriaService) {
        this.service = service;
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
        this.auditoriaService = auditoriaService;
    }

    private Usuario usuarioAutenticado() {
        Object principal = SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        String email = ((UserDetails) principal).getUsername();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }

    @GetMapping
    public List<Reserva> listar() {
        Usuario usuario = usuarioAutenticado();
        return service.listarPorUsuario(usuario.getId());
    }

    @GetMapping("/todas")
    public List<Reserva> listarTodas() {
        return service.listarTodas();
    }

    @GetMapping("/aprovadas")
    public List<Reserva> listarAprovadas() {
        return service.listarAprovadas();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reserva> buscarPorId(@PathVariable Long id) {
        Reserva reserva = service.buscarPorId(id);
        Usuario usuario = usuarioAutenticado();
        if (!service.podeGerenciar(usuario, reserva)) {
            throw new AccessDeniedException("Você não tem permissão para ver esta reserva");
        }
        return ResponseEntity.ok(reserva);
    }

    @PostMapping
    public ResponseEntity<Reserva> criar(@Valid @RequestBody ReservaRequest request) {
        Usuario usuario = usuarioAutenticado();
        Reserva salva = service.criar(request, usuario);
        return ResponseEntity.status(201).body(salva);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Reserva> atualizar(@PathVariable Long id, @Valid @RequestBody ReservaRequest request) {
        Usuario usuario = usuarioAutenticado();
        Reserva atualizada = service.atualizar(id, request, usuario);
        return ResponseEntity.ok(atualizada);
    }

    @PreAuthorize("hasAnyRole('GESTOR', 'ADMIN')")
    @PutMapping("/{id}/aprovar")
    public ResponseEntity<Reserva> aprovar(@PathVariable Long id) {
        Reserva reserva = service.aprovar(id);
        auditoriaService.registrar("RESERVA_APROVADA", "Reserva id=" + id);
        return ResponseEntity.ok(reserva);
    }

    @PreAuthorize("hasAnyRole('GESTOR', 'ADMIN')")
    @PutMapping("/{id}/rejeitar")
    public ResponseEntity<Reserva> rejeitar(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String motivo = body.getOrDefault("motivo", "");
        Reserva reserva = service.rejeitar(id, motivo);
        auditoriaService.registrar("RESERVA_REJEITADA", "Reserva id=" + id + "; motivo=" + motivo);
        return ResponseEntity.ok(reserva);
    }

    @PreAuthorize("hasAnyRole('GESTOR', 'ADMIN')")
    @GetMapping("/historico")
    public List<Reserva> listarHistorico() {
        return service.listarHistorico();
    }

    @PreAuthorize("hasAnyRole('GESTOR', 'ADMIN')")
    @PutMapping("/{id}/reverter")
    public ResponseEntity<Reserva> reverter(@PathVariable Long id) {
        Reserva reserva = service.reverter(id);
        auditoriaService.registrar("RESERVA_REVERTIDA", "Reserva id=" + id);
        return ResponseEntity.ok(reserva);
    }

    @PreAuthorize("hasAnyRole('GESTOR', 'ADMIN')")
    @PostMapping("/{id}/contatar")
    public ResponseEntity<Void> contatar(@PathVariable Long id,
                                         @RequestBody Map<String, String> body) {
        Reserva reserva = service.buscarPorId(id);
        Usuario gestor = usuarioAutenticado();

        emailService.enviarMensagemGestor(
                reserva.getUsuarioEmail(),
                reserva.getNome(),
                body.get("mensagem"),
                gestor.getNome()
        );
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/cancelar")
    public ResponseEntity<Reserva> cancelar(@PathVariable Long id) {
        Usuario usuario = usuarioAutenticado();
        Reserva reserva = service.cancelar(id, usuario);
        return ResponseEntity.ok(reserva);
    }

}
