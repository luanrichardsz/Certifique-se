package br.com.certifiquese.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.oauth2.jwt.Jwt;

import br.com.certifiquese.dto.CertificadoRequestDTO;
import br.com.certifiquese.dto.CertificadoResponseDTO;
import br.com.certifiquese.model.CertificadoEntity;
import br.com.certifiquese.model.UsuarioEntity;
import br.com.certifiquese.repository.CertificadoRepository;
import br.com.certifiquese.repository.UsuarioRepository;
import br.com.certifiquese.dto.CertificadoFiltroDTO;
import br.com.certifiquese.specification.CertificadoSpecification;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CertificadoService {
    
    private final CertificadoRepository certificadoRepository;
    private final UsuarioRepository usuarioRepository;

    public CertificadoService(CertificadoRepository certificadoRepository, UsuarioRepository usuarioRepository) {
        this.certificadoRepository = certificadoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public CertificadoResponseDTO cadastrar(Long idUsuario, CertificadoRequestDTO dto) {
        String hashCertificado = gerarHashCertificado(dto);

        if (certificadoRepository.existsByHashCertificado(hashCertificado)) {
            throw new IllegalArgumentException("Já existe um certificado cadastrado com este hash.");
        }

        UsuarioEntity usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado com ID: " + idUsuario));

        CertificadoEntity certificado = new CertificadoEntity();
        certificado.setHashCertificado(hashCertificado);
        certificado.setFoto(dto.foto());
        certificado.setNome(dto.nome());
        certificado.setEmpresa(dto.empresa());
        certificado.setDataConclusao(dto.dataConclusao());
        certificado.setTags(dto.tags());
        certificado.setUsuario(usuario);

        CertificadoEntity certificadoSalvo = certificadoRepository.save(certificado);

        return toResponseDTO(certificadoSalvo);
    }

    private String gerarHashCertificado(CertificadoRequestDTO dto) {
        String conteudo = String.join("|",
                dto.foto(),
                dto.nome(),
                dto.empresa(),
                dto.dataConclusao().toString(),
                dto.tags().stream().sorted().collect(Collectors.joining(","))
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

    private CertificadoResponseDTO toResponseDTO(CertificadoEntity certificado) {
        return new CertificadoResponseDTO(
                certificado.getIdCertificado(),
                certificado.getHashCertificado(),
                certificado.getFoto(),
                certificado.getNome(),
                certificado.getEmpresa(),
                certificado.getDataConclusao(),
                List.copyOf(certificado.getTags())
        );
    }
}
