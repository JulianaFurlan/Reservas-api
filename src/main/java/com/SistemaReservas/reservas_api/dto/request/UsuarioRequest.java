package com.SistemaReservas.reservas_api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UsuarioRequest {

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    private String email;

    private String telefone;
    private String departamento;

    @Pattern(regexp = "COMUM|GESTOR|ADMIN", message = "Tipo deve ser COMUM, GESTOR ou ADMIN")
    private String tipo;
}
