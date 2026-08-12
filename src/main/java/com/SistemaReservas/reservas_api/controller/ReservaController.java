package com.SistemaReservas.reservas_api.controller;

import com.SistemaReservas.reservas_api.model.Reserva;
import com.SistemaReservas.reservas_api.model.Usuario;
import com.SistemaReservas.reservas_api.repository.UsuarioRepository;
import com.SistemaReservas.reservas_api.service.EmailService;
import com.SistemaReservas.reservas_api.service.ReservaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservas")
@CrossOrigin(origins = "http://localhost:5173")
public class ReservaController {

    private final ReservaService service;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;

    public ReservaController(ReservaService service, UsuarioRepository usuarioRepository,  EmailService emailService) {
        this.service = service;
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
    }

    @GetMapping
    public List<Reserva> listar() {
        Object principal = SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        String email = ((UserDetails) principal).getUsername();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

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
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<Reserva> criar(@RequestBody Reserva reserva) {
        // Captura o usuário logado
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            String email = ((UserDetails) principal).getUsername();
            Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
            if (usuario != null) {
                reserva.setUsuarioId(usuario.getId());
                reserva.setUsuarioEmail(usuario.getEmail());
                reserva.setUsuarioNome(usuario.getNome());
            }
        }

        Reserva salva = service.salvar(reserva);
        return ResponseEntity.status(201).body(salva);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Reserva> atualizar(@PathVariable Long id, @RequestBody Reserva reserva) {
        reserva.setId(id);
        Reserva atualizada = service.salvar(reserva);
        return ResponseEntity.ok(atualizada);
    }

    @PutMapping("/{id}/aprovar")
    public ResponseEntity<Reserva> aprovar(@PathVariable Long id) {
        return ResponseEntity.ok(service.aprovar(id));
    }

    @PutMapping("/{id}/rejeitar")
    public ResponseEntity<Reserva> rejeitar(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String motivo = body.getOrDefault("motivo", "");
        return ResponseEntity.ok(service.rejeitar(id, motivo));
    }

    @GetMapping("/historico")
    public List<Reserva> listarHistorico() {
        return service.listarHistorico();
    }

    @PutMapping("/{id}/reverter")
    public ResponseEntity<Reserva> reverter(@PathVariable Long id) {
        return ResponseEntity.ok(service.reverter(id));
    }

    @PostMapping("/{id}/contatar")
    public ResponseEntity<Void> contatar(@PathVariable Long id,
                                         @RequestBody Map<String, String> body) {
        Reserva reserva = service.buscarPorId(id);

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String emailGestor = ((UserDetails) principal).getUsername();

        Usuario gestor = usuarioRepository.findByEmail(emailGestor)
                .orElseThrow(() -> new RuntimeException("Gestor não encontrado"));

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
        return ResponseEntity.ok(service.cancelar(id));
    }

}