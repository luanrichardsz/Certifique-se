package br.com.certifiquese.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "tb_usuario")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUsuario;

    @Column(nullable = false)
    private String nomeUsuario;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(length = 150)
    private String headline;

    private String biografia;

    @Column(length = 500)
    private String foto;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String senha;

    @Column(name = "perfil_publico", nullable = false)
    private Boolean perfilPublico = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column(nullable = false)
    private int tokenVersion;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL)
    private List<CertificadoEntity> certificados;

    @PrePersist
    public void antesDeSalvar(){
        if (role == null) {
            role = Role.USER;
        }
        if (perfilPublico == null) {
            perfilPublico = true;
        }
        criadoEm = LocalDateTime.now();
    }
}
