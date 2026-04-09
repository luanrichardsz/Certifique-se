package br.com.certifiquese.usuario;

import br.com.certifiquese.certificado.CertificadoEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "tb_usuario")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUsuario;

    @Column(nullable = false)
    private String nomeUsuario;
    private String biografia;

    @Column(nullable = false, unique =  true)
    private String email;

    @Column(nullable = false)
    private String senha;

    //mappedBy para falar pro spring que o usuario é a classe "pai", sem o mappedBy criaria uma nova tabela relacional das FK
    //Cascade define o que vamos conseguir mexer nesse objeto, o ALL Salva, Exclui e Merge (existe essas funcoes unicas no cascade)
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL)
    private List<CertificadoEntity> certificados;
}
