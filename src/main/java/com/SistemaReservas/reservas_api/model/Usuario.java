package com.SistemaReservas.reservas_api.model;

import com.SistemaReservas.reservas_api.model.enums.TipoUsuario;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String senha;

    private String telefone;
    private String departamento;

    @Enumerated(EnumType.STRING)
    private TipoUsuario tipo = TipoUsuario.COMUM;

    private Boolean ativo = true;

    private LocalDateTime dataCadastro = LocalDateTime.now();

    public String getRole() {
        return this.tipo.name();
    }
}