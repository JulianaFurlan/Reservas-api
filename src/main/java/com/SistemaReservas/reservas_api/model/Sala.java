package com.SistemaReservas.reservas_api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Entity
@NoArgsConstructor
@Table(name="salas")
public class Sala {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String bloco;

    @Column(nullable = false)
    private String tipo;

    private Integer capacidade;

    private String recursos;

    @Column(nullable = false)
    private String status = "ATIVO";

}
