package br.com.certifiquese.service;

import br.com.certifiquese.dto.UsuarioRequestDTO;
import br.com.certifiquese.dto.UsuarioResponseDTO;
import br.com.certifiquese.model.UsuarioEntity;
import br.com.certifiquese.repository.UsuarioRepository;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioResponseDTO cadastrar(UsuarioRequestDTO dto) {
        if (usuarioRepository.existsByEmail(dto.email())) {
            throw new IllegalArgumentException("Já existe um usuário cadastrado com este e-mail.");
        }

        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setNomeUsuario(dto.nomeUsuario());
        usuario.setEmail(dto.email());

        String senhaCriptografada = passwordEncoder.encode(dto.senha());
        usuario.setSenha(senhaCriptografada);

        usuario.setBiografia(dto.biografia());

        UsuarioEntity usuarioSalvo = usuarioRepository.save(usuario);

        return toResponseDTO(usuarioSalvo);
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorId(Long idUsuario) {
        UsuarioEntity usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        return toResponseDTO(usuario);
    }

    public List<UsuarioResponseDTO> listarTodos() {
        List<UsuarioEntity> usuarios = usuarioRepository.findAll();
        return usuarios.stream()
                .map(this::toResponseDTO)
                .toList();
    }

    private UsuarioResponseDTO toResponseDTO(UsuarioEntity usuario) {
        return new UsuarioResponseDTO(
                usuario.getIdUsuario(),
                usuario.getNomeUsuario(),
                usuario.getEmail(),
                usuario.getBiografia()
        );
    }
}