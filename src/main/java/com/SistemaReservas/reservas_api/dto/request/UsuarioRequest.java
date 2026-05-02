package com.SistemaReservas.reservas_api.dto.request;

import lombok.Data;

@Data
public class UsuarioRequest {
    private String nome;
    private String email;
    private String senha;
    private String telefone;
    private String departamento;
    private String tipo;  // COMUM, GESTOR, ADMIN
}