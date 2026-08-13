package br.com.certifiquese.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import br.com.certifiquese.model.CertificadoEntity;

public interface CertificadoRepository extends JpaRepository<CertificadoEntity, Long>, JpaSpecificationExecutor<CertificadoEntity> {

        Optional<CertificadoEntity> findByHashCertificado(String hashCertificado);

        // Verificação se já existe o hash do certificado cadastrado
        boolean existsByHashCertificado(String hashCertificado);

        // Buscar certificados por ID do usuário
        List<CertificadoEntity> findByUsuarioIdUsuario(Long idUsuario);
}
