package br.com.certifiquese.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "tb_certificado", uniqueConstraints = @UniqueConstraint(columnNames = "hash_certificado"))
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CertificadoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCertificado;

    @Column(name = "hash_certificado", nullable = false, unique = true)
    private String hashCertificado;

    @Column(nullable = false)
    private String foto;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String empresa;

    @Column(name = "data_conclusao", nullable = false)
    private LocalDate dataConclusao;

    @ElementCollection
    @CollectionTable(name = "tb_certificado_tag", joinColumns = @JoinColumn(name = "id_certificado"))
    @Column(name = "tag", nullable = false)
    private List<String> tags;

    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    private UsuarioEntity usuario;
}
