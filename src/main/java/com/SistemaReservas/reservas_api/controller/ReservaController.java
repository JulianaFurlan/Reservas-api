package com.SistemaReservas.reservas_api.controller;

import com.SistemaReservas.reservas_api.model.Reserva;
import com.SistemaReservas.reservas_api.model.Usuario;
import com.SistemaReservas.reservas_api.repository.UsuarioRepository;
import com.SistemaReservas.reservas_api.service.ReservaService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.SistemaReservas.reservas_api.model.enums.TipoUsuario;


import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservas")
@CrossOrigin(origins = "http://localhost:5173")
public class ReservaController {

    private final ReservaService service;
    private final UsuarioRepository usuarioRepository;

    public ReservaController(ReservaService service, UsuarioRepository usuarioRepository) {
        this.service = service;
        this.usuarioRepository = usuarioRepository;
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
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

    @PutMapping("/{id}/cancelar")
    public ResponseEntity<Reserva> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(service.cancelar(id));
    }

}