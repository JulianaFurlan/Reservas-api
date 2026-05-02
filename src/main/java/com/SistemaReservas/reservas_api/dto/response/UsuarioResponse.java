package com.SistemaReservas.reservas_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UsuarioResponse {
    private Long id;
    private String nome;
    private String email;
    private String telefone;
    private String departamento;
    private String tipo;
    private Boolean ativo;
}