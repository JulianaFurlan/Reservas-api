package com.SistemaReservas.reservas_api.exception;

import com.SistemaReservas.reservas_api.security.LoginBloqueadoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centraliza o tratamento de erros para que nenhuma resposta de API vaze detalhes
 * internos (stack trace, nome de classe, mensagens de SQL/JPA). Exceções de negócio
 * (RuntimeException lançadas deliberadamente pelos services, com mensagens já pensadas
 * para o usuário final) continuam expondo a mensagem; qualquer coisa inesperada é
 * logada no servidor e retorna uma mensagem genérica ao cliente.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(LoginBloqueadoException.class)
    public ResponseEntity<Map<String, String>> handleLoginBloqueado(LoginBloqueadoException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthentication(AuthenticationException e) {
        // Mensagem genérica propositalmente: não revela se o email existe, se a conta
        // está desativada ou se foi a senha que errou (evita enumeração de usuários).
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Email ou senha inválidos"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Acesso negado"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> erros = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(fieldError ->
                erros.put(fieldError.getField(), fieldError.getDefaultMessage()));
        return ResponseEntity.badRequest().body(erros);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleMalformedBody(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(Map.of("message", "Corpo da requisição inválido"));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception e) {
        log.error("Erro inesperado não tratado", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Erro interno. Tente novamente mais tarde."));
    }
}
