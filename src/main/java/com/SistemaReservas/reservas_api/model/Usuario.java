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

    private Boolean senhaTemporaria = false;

    // Incrementado ao trocar/resetar senha ou desativar a conta; usado para invalidar
    // tokens JWT emitidos antes dessa mudança (JWT é stateless e não tem revogação
    // nativa, então usamos esse "número de versão" como claim no token).
    private Integer tokenVersion = 0;

    public String getRole() {
        return this.tipo.name();
    }
}