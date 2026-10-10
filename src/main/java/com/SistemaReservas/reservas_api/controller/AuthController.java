package com.SistemaReservas.reservas_api.controller;

import com.SistemaReservas.reservas_api.dto.request.LoginRequest;
import com.SistemaReservas.reservas_api.dto.response.LoginResponse;
import com.SistemaReservas.reservas_api.dto.response.UsuarioLogadoResponse;
import com.SistemaReservas.reservas_api.dto.response.UsuarioResponse;
import com.SistemaReservas.reservas_api.model.Usuario;
import com.SistemaReservas.reservas_api.repository.UsuarioRepository;
import com.SistemaReservas.reservas_api.security.JwtUtil;
import com.SistemaReservas.reservas_api.security.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

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

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

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

        int tokenVersion = usuario.getTokenVersion() == null ? 0 : usuario.getTokenVersion();
        String token = jwtUtil.generateToken(usuario.getEmail(), usuario.getRole(), tokenVersion);

        UsuarioResponse usuarioResponse = new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getTelefone(),
                usuario.getDepartamento(),
                usuario.getTipo().toString(),
                usuario.getAtivo()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, criarCookieJwt(token, Duration.ofMillis(jwtExpirationMs)).toString())
                .body(new LoginResponse(usuarioResponse, Boolean.TRUE.equals(usuario.getSenhaTemporaria())));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, criarCookieJwt("", Duration.ZERO).toString())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            return naoAutenticado();
        }

        return usuarioRepository.findByEmail(userDetails.getUsername())
                .<ResponseEntity<?>>map(usuario -> ResponseEntity.ok(new UsuarioLogadoResponse(
                        usuario.getId(),
                        usuario.getNome(),
                        usuario.getEmail(),
                        usuario.getTipo().toString(),
                        Boolean.TRUE.equals(usuario.getSenhaTemporaria())
                )))
                .orElseGet(this::naoAutenticado);
    }

    private ResponseEntity<Map<String, String>> naoAutenticado() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Sessão expirada ou token inválido. Faça login novamente."));
    }

    private ResponseCookie criarCookieJwt(String valor, Duration maxAge) {
        return ResponseCookie.from("jwt", valor)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
