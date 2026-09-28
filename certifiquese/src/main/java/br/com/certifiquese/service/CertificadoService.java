package br.com.certifiquese.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import br.com.certifiquese.dto.CertificadoFiltroDTO;
import br.com.certifiquese.dto.CertificadoPublicoResponseDTO;
import br.com.certifiquese.dto.CertificadoRequestDTO;
import br.com.certifiquese.dto.CertificadoResponseDTO;
import br.com.certifiquese.dto.CertificadoUpdateDTO;
import br.com.certifiquese.dto.HashCertificadoProvider;
import br.com.certifiquese.exception.OperacaoNaoPermitidaException;
import br.com.certifiquese.exception.RecursoEmConflitoException;
import br.com.certifiquese.exception.RecursoNaoEncontradoException;
import br.com.certifiquese.model.CertificadoEntity;
import br.com.certifiquese.model.UsuarioEntity;
import br.com.certifiquese.repository.CertificadoRepository;
import br.com.certifiquese.repository.UsuarioRepository;
import br.com.certifiquese.specification.CertificadoSpecification;
import br.com.certifiquese.service.storage.CertificadoStorage;

@Service
public class CertificadoService {

    private static final Logger log = LoggerFactory.getLogger(CertificadoService.class);
    
    private final CertificadoRepository certificadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CertificadoStorage certificadoStorage;

    public CertificadoService(CertificadoRepository certificadoRepository,
                              UsuarioRepository usuarioRepository,
                              CertificadoStorage certificadoStorage) {
        this.certificadoRepository = certificadoRepository;
        this.usuarioRepository = usuarioRepository;
        this.certificadoStorage = certificadoStorage;
    }

    @Transactional
    public CertificadoResponseDTO cadastrar(Long idUsuario, CertificadoRequestDTO dto) {
        String hashCertificado = gerarHashCertificado((HashCertificadoProvider) dto);

        if (certificadoRepository.existsByHashCertificado(hashCertificado)) {
            throw new RecursoEmConflitoException("Já existe um certificado cadastrado com este hash.");
        }

        UsuarioEntity usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        CertificadoEntity certificado = new CertificadoEntity();
        certificado.setHashCertificado(hashCertificado);
        certificado.setFoto(dto.foto());
        certificado.setNome(dto.nome());
        certificado.setEmpresa(dto.empresa());
        certificado.setDataConclusao(dto.dataConclusao());
        certificado.setTags(dto.tags());
        certificado.setCargaHoraria(dto.cargaHoraria());
        certificado.setDescricao(dto.descricao());
        certificado.setLinkValidacao(dto.linkValidacao());
        certificado.setPublico(dto.publico() != null ? dto.publico() : true);
        certificado.setUsuario(usuario);

        CertificadoEntity certificadoSalvo = certificadoRepository.save(certificado);

        return toResponseDTO(certificadoSalvo);
    }

    @Transactional
    public CertificadoResponseDTO atualizar(Long idUsuario, String hashCertificado, CertificadoUpdateDTO dto) {
        CertificadoEntity certificado = certificadoRepository.findByHashCertificado(hashCertificado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Certificado não encontrado com o hash fornecido."));

        if (!certificado.getUsuario().getIdUsuario().equals(idUsuario)) {
            throw new OperacaoNaoPermitidaException("O usuário não tem permissão para editar este certificado.");
        }

        String novoHashCertificado = gerarHashCertificado((HashCertificadoProvider) dto);
        if (!hashCertificado.equals(novoHashCertificado) && certificadoRepository.existsByHashCertificado(novoHashCertificado)) {
            throw new RecursoEmConflitoException("Já existe um certificado cadastrado com este hash.");
        }

        String fotoAnterior = certificado.getFoto();

        certificado.setHashCertificado(novoHashCertificado);
        certificado.setFoto(dto.foto());
        certificado.setNome(dto.nome());
        certificado.setEmpresa(dto.empresa());
        certificado.setDataConclusao(dto.dataConclusao());
        certificado.setTags(dto.tags());
        certificado.setCargaHoraria(dto.cargaHoraria());
        certificado.setDescricao(dto.descricao());
        certificado.setLinkValidacao(dto.linkValidacao());
        if (dto.publico() != null) {
            certificado.setPublico(dto.publico());
        }

        CertificadoResponseDTO resposta = toResponseDTO(certificadoRepository.save(certificado));
        if (!Objects.equals(fotoAnterior, dto.foto())) {
            registrarRemocaoDeImagem(fotoAnterior);
        }
        return resposta;
    }

    private String gerarHashCertificado(HashCertificadoProvider provider) {
        String conteudo = String.join("|",
                provider.foto(),
                provider.nome(),
                provider.empresa(),
                provider.dataConclusao().toString(),
                provider.tags().stream().sorted().collect(Collectors.joining(","))
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(conteudo.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Falha ao gerar o hash do certificado.", e);
        }
    }

    @Transactional(readOnly = true)
    public List<CertificadoResponseDTO> listarTodos() {
        return certificadoRepository.findAll().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CertificadoResponseDTO> pesquisar(Jwt jwt, CertificadoFiltroDTO filtro) {
        Number usuarioIdClaim = jwt.getClaim("usuarioId");
        Long idUsuario = usuarioIdClaim.longValue();
        
        Specification<CertificadoEntity> spec = Specification.where(CertificadoSpecification.pertenceAoUsuario(idUsuario));

        if(filtro.nome() != null && !filtro.nome().isBlank()){
            spec = spec.and(CertificadoSpecification.nomeContem(filtro.nome().trim()));
        }

        if(filtro.empresa() != null && !filtro.empresa().isBlank()){
            spec = spec.and(CertificadoSpecification.empresaContem(filtro.empresa().trim()));
        }

        if(filtro.dataConclusao() != null){
            spec = spec.and(CertificadoSpecification.dataConclusaoIgual(filtro.dataConclusao()));
        }

        if(filtro.tags() != null && !filtro.tags().isBlank()){
            spec = spec.and(CertificadoSpecification.tagsContem(filtro.tags().trim()));
        }

        List<CertificadoEntity> certificados = certificadoRepository.findAll(spec);
        
        return certificados.stream().map(this::toResponseDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<CertificadoPublicoResponseDTO> listarPublicosPorUsername(String username) {
        String usernameNormalizado = username.trim().toLowerCase();
        UsuarioEntity usuario = usuarioRepository.findByUsername(usernameNormalizado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado com o username informado."));

        if (!Boolean.TRUE.equals(usuario.getPerfilPublico())) {
            throw new RecursoNaoEncontradoException("O portfólio deste usuário não está público.");
        }

        List<CertificadoEntity> publicos = certificadoRepository.findByUsuarioUsernameAndPublicoTrue(usernameNormalizado);
        return publicos.stream()
                .map(this::toPublicoResponseDTO)
                .toList();
    }

    public void deletar(Jwt jwt, String hashCertificado) {
        Number usuarioIdClaim = jwt.getClaim("usuarioId");
        Long idUsuario = usuarioIdClaim.longValue();

        CertificadoEntity certificado = certificadoRepository.findByHashCertificado(hashCertificado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Certificado não encontrado com o hash fornecido."));

        if (!certificado.getUsuario().getIdUsuario().equals(idUsuario)) {
            throw new OperacaoNaoPermitidaException("O usuário não tem permissão para deletar este certificado.");
        }

        certificadoRepository.delete(certificado);
        registrarRemocaoDeImagem(certificado.getFoto());
    }

    private CertificadoResponseDTO toResponseDTO(CertificadoEntity certificado) {
        return new CertificadoResponseDTO(
                certificado.getIdCertificado(),
                certificado.getHashCertificado(),
                certificado.getFoto(),
                certificado.getNome(),
                certificado.getEmpresa(),
                certificado.getDataConclusao(),
                List.copyOf(certificado.getTags()),
                certificado.getCargaHoraria(),
                certificado.getDescricao(),
                certificado.getLinkValidacao(),
                certificado.getPublico()
        );
    }

    private CertificadoPublicoResponseDTO toPublicoResponseDTO(CertificadoEntity certificado) {
        return new CertificadoPublicoResponseDTO(
                certificado.getHashCertificado(),
                certificado.getFoto(),
                certificado.getNome(),
                certificado.getEmpresa(),
                certificado.getDataConclusao(),
                List.copyOf(certificado.getTags()),
                certificado.getCargaHoraria(),
                certificado.getDescricao(),
                certificado.getLinkValidacao()
        );
    }

    private void registrarRemocaoDeImagem(String foto) {
        if (foto == null || foto.isBlank()) {
            return;
        }

        Runnable remocao = () -> certificadoStorage.remover(foto);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        remocao.run();
                    } catch (RuntimeException ex) {
                        log.warn("Falha ao remover imagem antiga do certificado no storage.", ex);
                    }
                }
            });
            return;
        }

        try {
            remocao.run();
        } catch (RuntimeException ex) {
            log.warn("Falha ao remover imagem antiga do certificado no storage.", ex);
        }
    }
}
