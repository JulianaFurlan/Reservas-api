package com.SistemaReservas.reservas_api.controller;

import com.SistemaReservas.reservas_api.dto.request.LoginRequest;
import com.SistemaReservas.reservas_api.dto.response.LoginResponse;
import com.SistemaReservas.reservas_api.dto.response.UsuarioResponse;
import com.SistemaReservas.reservas_api.model.Usuario;
import com.SistemaReservas.reservas_api.repository.UsuarioRepository;
import com.SistemaReservas.reservas_api.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        System.out.println("🔐 Tentativa de login: " + request.getEmail());
        // 1. Autentica o usuário
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getSenha()
                )
        );

        // 2. Busca o usuário no banco
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // 3. Gera o token JWT
        String token = jwtUtil.generateToken(usuario.getEmail(), usuario.getRole());

        // 4. CRIA O USUARIORESPONSE (CONVERTE Usuario PARA UsuarioResponse)
        UsuarioResponse usuarioResponse = new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getTelefone(),
                usuario.getDepartamento(),
                usuario.getTipo().toString(),
                usuario.getAtivo()
        );

        // 5. Retorna o LoginResponse com token e usuarioResponse
        return ResponseEntity.ok(new LoginResponse(token, usuarioResponse));
    }
}