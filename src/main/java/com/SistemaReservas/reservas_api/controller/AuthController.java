package com.SistemaReservas.reservas_api.controller;

import com.SistemaReservas.reservas_api.dto.request.LoginRequest;
import com.SistemaReservas.reservas_api.dto.response.LoginResponse;
import com.SistemaReservas.reservas_api.dto.response.UsuarioResponse;
import com.SistemaReservas.reservas_api.model.Usuario;
import com.SistemaReservas.reservas_api.repository.UsuarioRepository;
import com.SistemaReservas.reservas_api.security.JwtUtil;
import com.SistemaReservas.reservas_api.security.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String email = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase();
        String chaveEmail = "email:" + email;
        String chaveIp = "ip:" + httpRequest.getRemoteAddr();

        loginAttemptService.verificarBloqueio(chaveEmail);
        loginAttemptService.verificarBloqueio(chaveIp);

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getSenha())
            );
        } catch (AuthenticationException e) {
            loginAttemptService.registrarFalha(chaveEmail);
            loginAttemptService.registrarFalha(chaveIp);
            throw e;
        }

        loginAttemptService.registrarSucesso(chaveEmail);
        loginAttemptService.registrarSucesso(chaveIp);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        log.info("Login bem-sucedido para usuário id={}", usuario.getId());

        String token = jwtUtil.generateToken(usuario.getEmail(), usuario.getRole(), usuario.getTokenVersion());

        UsuarioResponse usuarioResponse = new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getTelefone(),
                usuario.getDepartamento(),
                usuario.getTipo().toString(),
                usuario.getAtivo()
        );

        return ResponseEntity.ok(new LoginResponse(token, usuarioResponse));
    }
}
