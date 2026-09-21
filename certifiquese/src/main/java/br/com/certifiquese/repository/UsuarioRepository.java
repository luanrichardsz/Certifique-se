package br.com.certifiquese.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.certifiquese.model.UsuarioEntity;

public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long> {
    Optional<UsuarioEntity> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdUsuarioNot(String email, Long idUsuario);

    Optional<UsuarioEntity> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByUsernameAndIdUsuarioNot(String username, Long idUsuario);
}
