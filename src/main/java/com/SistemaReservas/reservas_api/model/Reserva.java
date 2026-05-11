package com.SistemaReservas.reservas_api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "reservas")
@Data
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private LocalTime horaInicio;

    @Column(nullable = false)
    private LocalTime horaFim;

    @Column(nullable = false)
    private String sala;

    private String nome;
    private String email;
    private String telefone;
    private String departamento;
    private String finalidade;
    private String observacoes;
    private String status = "PENDENTE";
    private Long usuarioId;
    private String usuarioNome;
    private String usuarioEmail;
}