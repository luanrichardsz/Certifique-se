package br.com.certifiquese.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.certifiquese.model.RecuperacaoSenhaEntity;

public interface RecuperacaoSenhaRepository extends JpaRepository<RecuperacaoSenhaEntity, Long> {

    Optional<RecuperacaoSenhaEntity> findByHashToken(String hashToken);

    List<RecuperacaoSenhaEntity> findByUsuarioIdUsuarioAndUtilizadoEmIsNull(Long idUsuario);
}