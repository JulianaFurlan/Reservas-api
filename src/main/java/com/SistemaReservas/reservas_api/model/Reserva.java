package com.SistemaReservas.reservas_api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sala_id", insertable = false, updatable = false)
    private Sala sala;

    @Column(name = "sala_id", nullable = false)
    private Long salaId;

    @Column(name = "sala")
    private String salaNome;

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
    private String motivoRejeicao;
}