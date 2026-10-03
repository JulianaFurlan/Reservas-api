package com.SistemaReservas.reservas_api.controller;

import com.SistemaReservas.reservas_api.dto.request.AlterarSenhaRequest;
import com.SistemaReservas.reservas_api.dto.request.UsuarioRequest;
import com.SistemaReservas.reservas_api.dto.response.UsuarioResponse;
import com.SistemaReservas.reservas_api.model.Usuario;
import com.SistemaReservas.reservas_api.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    private UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getTelefone(),
                usuario.getDepartamento(),
                usuario.getTipo().toString(),
                usuario.getAtivo()
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<UsuarioResponse> listar() {
        return service.listarTodos().stream().map(this::toResponse).toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<UsuarioResponse> cadastrar(@Valid @RequestBody UsuarioRequest request) {
        Usuario salvo = service.cadastrar(request);
        return ResponseEntity.status(201).body(toResponse(salvo));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> editar(@PathVariable Long id,
                                                  @Valid @RequestBody UsuarioRequest request) {
        Usuario atualizado = service.editar(id, request);
        return ResponseEntity.ok(toResponse(atualizado));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/ativo")
    public ResponseEntity<UsuarioResponse> alterarAtivo(@PathVariable Long id,
                                                        @RequestBody Map<String, Boolean> body) {
        Usuario atualizado = service.alterarAtivo(id, body.get("ativo"));
        return ResponseEntity.ok(toResponse(atualizado));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/resetar-senha")
    public ResponseEntity<Map<String, String>> resetarSenha(@PathVariable Long id) {
        String senhaTemp = service.resetarSenha(id);
        return ResponseEntity.ok(Map.of("senhaTemporaria", senhaTemp));
    }

    @PutMapping("/perfil/senha")
    public ResponseEntity<Void> alterarSenha(@Valid @RequestBody AlterarSenhaRequest request) {
        Object principal = SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        String email = ((UserDetails) principal).getUsername();

        service.alterarSenha(email, request.getSenhaAtual(), request.getNovaSenha());
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
