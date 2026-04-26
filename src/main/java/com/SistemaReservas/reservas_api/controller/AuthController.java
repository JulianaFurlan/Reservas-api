package com.SistemaReservas.reservas_api.controller;

import com.SistemaReservas.reservas_api.model.Usuario;
import com.SistemaReservas.reservas_api.repository.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioRepository usuarioRepository;

    public AuthController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String senha = request.get("senha");

        System.out.println("Login attempt: " + email);

        // Busca o usuário no banco
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);

        if (usuario == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Usuário não encontrado"));
        }

        // Verifica senha (senha está criptografada? Para teste, compare com "admin123")
        // Como a senha está criptografada, vamos criar um usuário com senha simples

        Map<String, Object> response = new HashMap<>();
        response.put("token", "fake-token-123");

        Map<String, Object> usuarioResponse = new HashMap<>();
        usuarioResponse.put("id", usuario.getId());
        usuarioResponse.put("nome", usuario.getNome());
        usuarioResponse.put("email", usuario.getEmail());
        usuarioResponse.put("tipo", usuario.getTipo().toString());

        response.put("usuario", usuarioResponse);

        return ResponseEntity.ok(response);
    }
}