package com.SistemaReservas.reservas_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AlterarSenhaRequest {

    @NotBlank(message = "Senha atual é obrigatória")
    private String senhaAtual;

    @NotBlank(message = "Nova senha é obrigatória")
    @Size(min = 8, message = "A nova senha deve ter pelo menos 8 caracteres")
    private String novaSenha;
}
