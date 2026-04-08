package br.com.certifiquese.certificado;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "tb_certificado")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CertificadoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCertificado;

    @Column(name = "hash_certificado", unique = true)
    private String hashCertificado;

    @Column(name = "url_certificado", nullable = false)
    private String urlCertificado;

    @Column(name = "assunto_certificado", nullable = false)
    private String assuntoCertificado;

    @Column(name = "dt_emissao", nullable = false)
    private LocalDate emissao;

    @Column(name = "dt_vencimento")
    private LocalDate vencimento;

    private int cargaHoraria;

//    @ManyToOne
//    public UsuarioEntity usuario;
}
