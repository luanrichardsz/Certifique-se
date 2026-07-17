package br.com.certifiquese.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.certifiquese.model.CertificadoEntity;

public interface CertificadoRepository extends JpaRepository<CertificadoEntity, Long> {

        Optional<CertificadoEntity> findByHashCertificado(String hashCertificado);

        // Verificação se já existe o hash do certificado cadastrado
        boolean existsByHashCertificado(String hashCertificado);

        // Buscar certificados por ID do usuário
        List<CertificadoEntity> findByUsuarioIdUsuario(Long idUsuario);

        // Buscar certificados por ID do usuário e nome do certificado
        List<CertificadoEntity> findByUsuarioIdUsuarioAndNomeContainingIgnoreCase(Long idUsuario, String nome);

        // Buscar certificados por ID do usuário e empresa
        List<CertificadoEntity> findByUsuarioIdUsuarioAndEmpresaContainingIgnoreCase(Long idUsuario, String empresa);

        // Buscar certificados por ID do usuário e data de conclusão
        List<CertificadoEntity> findByUsuarioIdUsuarioAndDataConclusao(Long idUsuario, LocalDate dataConclusao);

        // Buscar certificados por ID do usuario e tag (Como tags é uma collection,
        // precisa do @Query para fazer consulta em outra tabela)
        @Query("""
                        select distinct certificado
                        from CertificadoEntity certificado
                        join certificado.tags tag
                        where certificado.usuario.idUsuario = :idUsuario
                          and lower(tag) like lower(concat('%', :tag, '%'))
                        """)
        List<CertificadoEntity> findByUsuarioIdUsuarioAndTagContainingIgnoreCase(
                        @Param("idUsuario") Long idUsuario,
                        @Param("tag") String tag);
}
