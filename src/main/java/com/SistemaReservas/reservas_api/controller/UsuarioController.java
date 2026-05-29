package com.SistemaReservas.reservas_api.controller;

import com.SistemaReservas.reservas_api.model.Usuario;
import com.SistemaReservas.reservas_api.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "http://localhost:5173")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<Usuario> listar() {
        return service.listarTodos();
    }

    @PostMapping
    public ResponseEntity<Usuario> cadastrar(@RequestBody Usuario usuario) {
        return ResponseEntity.status(201).body(service.cadastrar(usuario));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Usuario> editar(@PathVariable Long id,
                                          @RequestBody Usuario dados) {
        return ResponseEntity.ok(service.editar(id, dados));
    }

    @PutMapping("/{id}/ativo")
    public ResponseEntity<Usuario> alterarAtivo(@PathVariable Long id,
                                                @RequestBody Map<String, Boolean> body) {
        return ResponseEntity.ok(service.alterarAtivo(id, body.get("ativo")));
    }

    @PutMapping("/{id}/resetar-senha")
    public ResponseEntity<Map<String, String>> resetarSenha(@PathVariable Long id) {
        String senhaTemp = service.resetarSenha(id);
        return ResponseEntity.ok(Map.of("senhaTemporaria", senhaTemp));
    }
}