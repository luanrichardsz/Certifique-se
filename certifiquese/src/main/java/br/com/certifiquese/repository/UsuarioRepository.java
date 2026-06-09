package br.com.certifiquese.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.certifiquese.model.UsuarioEntity;

public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long> {
    Optional<UsuarioEntity> findByEmail(String email);

    // Verificação se já existe o email cadastrado
    boolean existsByEmail(String email);
}