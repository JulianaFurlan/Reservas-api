package com.SistemaReservas.reservas_api.dto.response;

import lombok.Data;

@Data

public class LoginResponse {
    private String token;
    private UsuarioResponse usuario;
    private boolean senhaTemporaria;

    public LoginResponse(String token, UsuarioResponse usuarioResponse) {
        this.token = token;
        this.usuario = usuarioResponse;
    }
}