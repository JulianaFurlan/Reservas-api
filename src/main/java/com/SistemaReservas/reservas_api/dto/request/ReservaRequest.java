package com.SistemaReservas.reservas_api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

// status, usuarioId, usuarioEmail e usuarioNome não existem aqui de propósito: são
// sempre definidos pelo backend (ver ReservaService) para evitar mass assignment,
// como um usuário comum criando a própria reserva já "APROVADA".
@Data
public class ReservaRequest {

    @NotNull(message = "Data é obrigatória")
    private LocalDate data;

    @NotNull(message = "Hora de início é obrigatória")
    private LocalTime horaInicio;

    @NotNull(message = "Hora de fim é obrigatória")
    private LocalTime horaFim;

    @NotNull(message = "Sala é obrigatória")
    private Long salaId;

    private String nome;
    private String email;
    private String telefone;
    private String departamento;
    private String finalidade;
    private String observacoes;
}
