package br.com.certifiquese.service;

import br.com.certifiquese.dto.AlterarSenhaRequestDTO;
import br.com.certifiquese.dto.PerfilPublicoResponseDTO;
import br.com.certifiquese.dto.UsuarioRequestDTO;
import br.com.certifiquese.dto.UsuarioResponseDTO;
import br.com.certifiquese.dto.UsuarioUpdateDTO;
import br.com.certifiquese.exception.RecursoEmConflitoException;
import br.com.certifiquese.exception.RecursoNaoEncontradoException;
import br.com.certifiquese.exception.SenhaIncorretaException;
import br.com.certifiquese.exception.TokenInvalidoException;
import br.com.certifiquese.model.Role;
import br.com.certifiquese.model.CertificadoEntity;
import br.com.certifiquese.model.UsuarioEntity;
import br.com.certifiquese.repository.CertificadoRepository;
import br.com.certifiquese.repository.UsuarioRepository;
import br.com.certifiquese.service.storage.CertificadoStorage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);
    private static final long TAMANHO_MAXIMO_FOTO_PERFIL = 3L * 1024 * 1024;

    private final UsuarioRepository usuarioRepository;
    private final CertificadoRepository certificadoRepository;
    private final PasswordEncoder passwordEncoder;
    private final CertificadoStorage certificadoStorage;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          CertificadoRepository certificadoRepository,
                          PasswordEncoder passwordEncoder,
                          CertificadoStorage certificadoStorage) {
        this.usuarioRepository = usuarioRepository;
        this.certificadoRepository = certificadoRepository;
        this.passwordEncoder = passwordEncoder;
        this.certificadoStorage = certificadoStorage;
    }

    @Transactional
    public UsuarioResponseDTO cadastrar(UsuarioRequestDTO dto) {
        if (usuarioRepository.existsByEmail(dto.email())) {
            throw new RecursoEmConflitoException("Já existe um usuário cadastrado com este e-mail.");
        }

        String username = dto.username();
        if (username == null || username.isBlank()) {
            username = gerarUsernameUnico(dto.email());
        } else {
            username = username.trim().toLowerCase();
            if (usuarioRepository.existsByUsername(username)) {
                throw new RecursoEmConflitoException("Já existe um usuário cadastrado com este username.");
            }
        }

        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setNomeUsuario(dto.nomeUsuario());
        usuario.setUsername(username);
        usuario.setHeadline(dto.headline());
        usuario.setEmail(dto.email());
        usuario.setRole(Role.USER);
        usuario.setPerfilPublico(true);
        usuario.setCriadoEm(LocalDateTime.now());

        String senhaCriptografada = passwordEncoder.encode(dto.senha());
        usuario.setSenha(senhaCriptografada);

        usuario.setBiografia(dto.biografia());

        UsuarioEntity usuarioSalvo = usuarioRepository.save(usuario);

        return toResponseDTO(usuarioSalvo);
    }

    @Transactional
    public UsuarioResponseDTO atualizar(Jwt jwt, UsuarioUpdateDTO dto) {
        Long idUsuario = obterUsuarioId(jwt);

        UsuarioEntity usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        if (!usuario.getEmail().equals(dto.email()) && usuarioRepository.existsByEmailAndIdUsuarioNot(dto.email(), idUsuario)) {
            throw new RecursoEmConflitoException("Já existe um usuário cadastrado com este e-mail.");
        }

        if (dto.username() != null && !dto.username().isBlank()) {
            String novoUsername = dto.username().trim().toLowerCase();
            if (!novoUsername.equals(usuario.getUsername()) && usuarioRepository.existsByUsernameAndIdUsuarioNot(novoUsername, idUsuario)) {
                throw new RecursoEmConflitoException("Já existe um usuário cadastrado com este username.");
            }
            usuario.setUsername(novoUsername);
        }

        usuario.setNomeUsuario(dto.nomeUsuario());
        usuario.setEmail(dto.email());
        usuario.setHeadline(dto.headline());
        usuario.setBiografia(dto.biografia());
        if (dto.perfilPublico() != null) {
            usuario.setPerfilPublico(dto.perfilPublico());
        }
        if (dto.foto() != null) {
            usuario.setFoto(dto.foto().isBlank() ? null : dto.foto().trim());
        }

        return toResponseDTO(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponseDTO atualizarFoto(Jwt jwt, MultipartFile arquivo) {
        Long idUsuario = obterUsuarioId(jwt);
        UsuarioEntity usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Selecione uma imagem para enviar.");
        }

        if (arquivo.getSize() > TAMANHO_MAXIMO_FOTO_PERFIL) {
            throw new IllegalArgumentException("A foto de perfil deve ter no máximo 3MB.");
        }

        String fotoAntiga = usuario.getFoto();
        CertificadoStorage.ImagemArmazenada imagemArmazenada = certificadoStorage.armazenar(arquivo, "perfil");

        usuario.setFoto(imagemArmazenada.url());
        UsuarioEntity salvo = usuarioRepository.save(usuario);

        if (fotoAntiga != null && !fotoAntiga.isBlank()) {
            try {
                certificadoStorage.remover(fotoAntiga);
            } catch (Exception ex) {
                log.warn("Falha ao remover foto anterior de perfil do usuário id={}", idUsuario, ex);
            }
        }

        return toResponseDTO(salvo);
    }

    @Transactional
    public UsuarioResponseDTO removerFoto(Jwt jwt) {
        Long idUsuario = obterUsuarioId(jwt);
        UsuarioEntity usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        String fotoAntiga = usuario.getFoto();
        usuario.setFoto(null);
        UsuarioEntity salvo = usuarioRepository.save(usuario);

        if (fotoAntiga != null && !fotoAntiga.isBlank()) {
            try {
                certificadoStorage.remover(fotoAntiga);
            } catch (Exception ex) {
                log.warn("Falha ao remover foto de perfil do usuário id={}", idUsuario, ex);
            }
        }

        return toResponseDTO(salvo);
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorId(Long idUsuario) {
        UsuarioEntity usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        return toResponseDTO(usuario);
    }

    @Transactional(readOnly = true)
    public PerfilPublicoResponseDTO buscarPerfilPublico(String username) {
        String usernameNormalizado = username.trim().toLowerCase();
        UsuarioEntity usuario = usuarioRepository.findByUsername(usernameNormalizado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Perfil público não encontrado para o usuário informado."));

        if (!Boolean.TRUE.equals(usuario.getPerfilPublico())) {
            throw new RecursoNaoEncontradoException("Este usuário desativou a visualização pública do perfil.");
        }

        List<CertificadoEntity> certificadosPublicos = certificadoRepository.findByUsuarioUsernameAndPublicoTrue(usernameNormalizado);
        int totalCertificados = certificadosPublicos.size();
        int totalHoras = certificadosPublicos.stream()
                .filter(c -> c.getCargaHoraria() != null)
                .mapToInt(CertificadoEntity::getCargaHoraria)
                .sum();

        return new PerfilPublicoResponseDTO(
                usuario.getNomeUsuario(),
                usuario.getUsername(),
                usuario.getHeadline(),
                usuario.getBiografia(),
                usuario.getFoto(),
                usuario.getCriadoEm(),
                totalCertificados,
                totalHoras
        );
    }

    @Transactional
    public void alterarSenha(Jwt jwt, AlterarSenhaRequestDTO dto) {
        Long idUsuario = obterUsuarioId(jwt);

        UsuarioEntity usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        if (!passwordEncoder.matches(dto.senhaAtual(), usuario.getSenha())) {
            throw new SenhaIncorretaException("A senha atual informada está incorreta.");
        }

        usuario.setSenha(passwordEncoder.encode(dto.novaSenha()));
        usuario.setTokenVersion(usuario.getTokenVersion() + 1);
        usuarioRepository.save(usuario);
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
                .map(CertificadoEntity::getFoto)
                .toList();

        if (!certificadosDoUsuario.isEmpty()) {
            certificadoRepository.deleteAll(certificadosDoUsuario);
            certificadoRepository.flush();
        }

        List<String> fotosParaLimpar = new ArrayList<>(fotosDosCertificados);
        if (usuario.getFoto() != null && !usuario.getFoto().isBlank()) {
            fotosParaLimpar.add(usuario.getFoto());
        }

        usuarioRepository.delete(usuario);
        usuarioRepository.flush();

        registrarLimpezaDeImagens(fotosParaLimpar);
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
                usuario.getUsername(),
                usuario.getEmail(),
                usuario.getHeadline(),
                usuario.getBiografia(),
                usuario.getFoto(),
                usuario.getPerfilPublico(),
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

    private String gerarUsernameUnico(String email) {
        String base = email.split("@")[0].toLowerCase().replaceAll("[^a-z0-9._-]", "");
        if (base.length() < 3) {
            base = "user" + base;
        }
        String candidato = base;
        int sufixo = 1;
        while (usuarioRepository.existsByUsername(candidato)) {
            candidato = base + sufixo;
            sufixo++;
        }
        return candidato;
    }

    private void registrarLimpezaDeImagens(List<String> fotosDosCertificados) {
        if (fotosDosCertificados.isEmpty()) {
            return;
        }

        Runnable limpeza = () -> fotosDosCertificados.forEach(certificadoStorage::remover);

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
