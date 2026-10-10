package com.SistemaReservas.reservas_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UsuarioLogadoResponse {
    private Long id;
    private String nome;
    private String email;
    private String tipo;
    private boolean senhaTemporaria;
}
