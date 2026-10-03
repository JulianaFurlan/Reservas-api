package com.SistemaReservas.reservas_api.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "registros_auditoria")
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @Column(nullable = false)
    private String usuarioEmail;

    @Column(nullable = false)
    private String acao;

    @Column(length = 1000)
    private String detalhe;

    public RegistroAuditoria(String usuarioEmail, String acao, String detalhe) {
        this.dataHora = LocalDateTime.now();
        this.usuarioEmail = usuarioEmail;
        this.acao = acao;
        this.detalhe = detalhe;
    }
}
