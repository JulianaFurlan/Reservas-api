package com.SistemaReservas.reservas_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private UsuarioResponse usuario;

    @Data
    @AllArgsConstructor
    public static class UsuarioResponse {
        private Long id;
        private String nome;
        private String email;
        private String tipo;
    }
}