package com.SistemaReservas.reservas_api.dto.response;

import lombok.Data;

@Data
public class LoginResponse {
    private UsuarioResponse usuario;
    private boolean senhaTemporaria;

    public LoginResponse(UsuarioResponse usuarioResponse, boolean senhaTemporaria) {
        this.usuario = usuarioResponse;
        this.senhaTemporaria = senhaTemporaria;
    }
}