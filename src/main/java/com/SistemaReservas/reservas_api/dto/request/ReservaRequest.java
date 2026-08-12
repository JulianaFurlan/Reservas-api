package com.SistemaReservas.reservas_api.dto.request;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ReservaRequest {
    private LocalDate data;
    private LocalTime horaInicio;
    private LocalTime horaFim;
    private Long salaId;
    private String nome;
    private String email;
    private String telefone;
    private String departamento;
    private String finalidade;
    private String observacoes;
    private Long usuarioId;
    private String usuarioEmail;
    private String usuarioNome;
}
