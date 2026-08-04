package br.com.certifiquese.service;

import br.com.certifiquese.dto.UsuarioRequestDTO;
import br.com.certifiquese.dto.UsuarioResponseDTO;
import br.com.certifiquese.exception.RecursoEmConflitoException;
import br.com.certifiquese.exception.RecursoNaoEncontradoException;
import br.com.certifiquese.exception.SenhaIncorretaException;
import br.com.certifiquese.exception.TokenInvalidoException;
import br.com.certifiquese.model.Role;
import br.com.certifiquese.model.CertificadoEntity;
import br.com.certifiquese.model.UsuarioEntity;
import br.com.certifiquese.repository.CertificadoRepository;
import br.com.certifiquese.repository.UsuarioRepository;
import br.com.certifiquese.service.storage.SupabaseStorageService;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final CertificadoRepository certificadoRepository;
    private final PasswordEncoder passwordEncoder;
    private final SupabaseStorageService supabaseStorageService;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          CertificadoRepository certificadoRepository,
                          PasswordEncoder passwordEncoder,
                          SupabaseStorageService supabaseStorageService) {
        this.usuarioRepository = usuarioRepository;
        this.certificadoRepository = certificadoRepository;
        this.passwordEncoder = passwordEncoder;
        this.supabaseStorageService = supabaseStorageService;
    }

    @Transactional
    public UsuarioResponseDTO cadastrar(UsuarioRequestDTO dto) {
        if (usuarioRepository.existsByEmail(dto.email())) {
            throw new RecursoEmConflitoException("Já existe um usuário cadastrado com este e-mail.");
        }

        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setNomeUsuario(dto.nomeUsuario());
        usuario.setEmail(dto.email());
        usuario.setRole(Role.USER);
        usuario.setCriadoEm(LocalDateTime.now());

        String senhaCriptografada = passwordEncoder.encode(dto.senha());
        usuario.setSenha(senhaCriptografada);

        usuario.setBiografia(dto.biografia());

        UsuarioEntity usuarioSalvo = usuarioRepository.save(usuario);

        return toResponseDTO(usuarioSalvo);
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorId(Long idUsuario) {
        UsuarioEntity usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        return toResponseDTO(usuario);
    }

    @Transactional
    public void excluirConta(Jwt jwt, String senhaAtual) {
        Long usuarioId = obterUsuarioId(jwt);

        UsuarioEntity usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        if (!passwordEncoder.matches(senhaAtual, usuario.getSenha())) {
            throw new SenhaIncorretaException("A senha atual informada está incorreta.");
        }

        List<CertificadoEntity> certificadosDoUsuario = certificadoRepository.findByUsuarioIdUsuario(usuarioId);
        List<String> fotosDosCertificados = certificadosDoUsuario.stream()
            .filter(certificado -> certificado.getFoto() != null && !certificado.getFoto().isBlank())
            .map(certificado -> certificado.getFoto())
                .toList();

        if (!certificadosDoUsuario.isEmpty()) {
            certificadoRepository.deleteAll(certificadosDoUsuario);
            certificadoRepository.flush();
        }

        usuarioRepository.delete(usuario);
        usuarioRepository.flush();

        registrarLimpezaDeImagens(fotosDosCertificados);
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
            usuario.getBiografia(),
            usuario.getRole(),
            usuario.getCriadoEm()
        );
    }

    private Long obterUsuarioId(Jwt jwt) {
        Number usuarioIdClaim = jwt.getClaim("usuarioId");

        if (usuarioIdClaim == null) {
            throw new TokenInvalidoException("Token inválido: claim usuarioId ausente.");
        }

        return usuarioIdClaim.longValue();
    }

    private void registrarLimpezaDeImagens(List<String> fotosDosCertificados) {
        if (fotosDosCertificados.isEmpty()) {
            return;
        }

        Runnable limpeza = () -> fotosDosCertificados.forEach(supabaseStorageService::removerFoto);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        limpeza.run();
                    } catch (RuntimeException ex) {
                        log.warn("Falha ao remover imagens do storage após excluir a conta do usuário.", ex);
                    }
                }
            });
            return;
        }

        try {
            limpeza.run();
        } catch (RuntimeException ex) {
            log.warn("Falha ao remover imagens do storage após excluir a conta do usuário.", ex);
        }
    }
}