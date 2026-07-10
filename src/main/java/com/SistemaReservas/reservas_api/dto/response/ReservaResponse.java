package com.SistemaReservas.reservas_api.dto.response;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ReservaResponse {
    private Long id;
    private LocalDate data;
    private LocalTime horaInicio;
    private LocalTime horaFim;
    private Long salaId;
    private String salaNome;
    private String salaBloco;
    private String nome;
    private String email;
    private String telefone;
    private String departamento;
    private String finalidade;
    private String observacoes;
    private String status;
    private String motivoRejeicao;
    private Long usuarioId;
    private String usuarioNome;
    private String usuarioEmail;
}
