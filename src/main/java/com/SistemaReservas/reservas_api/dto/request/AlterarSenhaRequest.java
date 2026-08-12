package com.SistemaReservas.reservas_api.dto.request;

import lombok.Data;


@Data
public class AlterarSenhaRequest {
    private String senhaAtual;
    private String novaSenha;
}
