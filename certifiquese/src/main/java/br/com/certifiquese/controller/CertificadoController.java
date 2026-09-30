package br.com.certifiquese.controller;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import br.com.certifiquese.dto.CertificadoExtracaoResponseDTO;
import br.com.certifiquese.dto.CertificadoFiltroDTO;
import br.com.certifiquese.dto.CertificadoRequestDTO;
import br.com.certifiquese.dto.CertificadoResponseDTO;
import br.com.certifiquese.dto.CertificadoUpdateDTO;
import br.com.certifiquese.dto.ImagemUploadResponseDTO;
import br.com.certifiquese.service.CertificadoService;
import br.com.certifiquese.service.GeminiService;
import br.com.certifiquese.service.storage.CertificadoStorage;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/certificados")
public class CertificadoController {
    
    private final CertificadoService certificadoService;
    private final CertificadoStorage certificadoStorage;
    private final GeminiService geminiService;

    public CertificadoController(CertificadoService certificadoService, CertificadoStorage certificadoStorage, GeminiService geminiService) {
        this.certificadoService = certificadoService;
        this.certificadoStorage = certificadoStorage;
        this.geminiService = geminiService;
    }

    @PostMapping(value = "/extrair-dados", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CertificadoExtracaoResponseDTO> extrairDados(@RequestParam("arquivo") MultipartFile arquivo) {
        CertificadoStorage.ImagemArmazenada imagem = certificadoStorage.armazenar(arquivo);
        CertificadoExtracaoResponseDTO resposta = geminiService != null
                ? geminiService.extrairDados(arquivo, imagem.chave(), imagem.url())
                : CertificadoExtracaoResponseDTO.vazio(imagem.chave(), imagem.url());
        return ResponseEntity.ok(resposta);
    }

    @PostMapping(value = "/imagens", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImagemUploadResponseDTO> enviarImagem(@RequestParam("arquivo") MultipartFile arquivo) {
        CertificadoStorage.ImagemArmazenada imagem = certificadoStorage.armazenar(arquivo);
        return ResponseEntity.status(201).body(new ImagemUploadResponseDTO(imagem.chave(), imagem.url()));
    }

    @GetMapping("/imagens/{chave:.+}")
    public ResponseEntity<byte[]> buscarImagem(@PathVariable String chave) {
        CertificadoStorage.ArquivoArmazenado arquivo = certificadoStorage.buscar(chave);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(arquivo.contentType()))
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
                .body(arquivo.conteudo());
    }

    @PostMapping
    public ResponseEntity<CertificadoResponseDTO> cadastrar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CertificadoRequestDTO dto) {
        Number usuarioIdClaim = jwt.getClaim("usuarioId");
        Long usuarioId = usuarioIdClaim.longValue();
        
        CertificadoResponseDTO certificadoSalvo = certificadoService.cadastrar(usuarioId, dto);
        return ResponseEntity.status(201).body(certificadoSalvo);
    }

    @RequestMapping(value = "/{hashCertificado}", method = RequestMethod.PUT)
    public ResponseEntity<CertificadoResponseDTO> atualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable String hashCertificado, @Valid @RequestBody CertificadoUpdateDTO dto) {
        Number usuarioIdClaim = jwt.getClaim("usuarioId");
        Long usuarioId = usuarioIdClaim.longValue();

        CertificadoResponseDTO certificadoAtualizado = certificadoService.atualizar(usuarioId, hashCertificado, dto);
        return ResponseEntity.ok(certificadoAtualizado);
    }

    @GetMapping
    public ResponseEntity<List<CertificadoResponseDTO>> listarTodos() {
        List<CertificadoResponseDTO> certificados = certificadoService.listarTodos();
        return ResponseEntity.ok(certificados);
    }

    @GetMapping("/me")
    public ResponseEntity<List<CertificadoResponseDTO>> pesquisar(@AuthenticationPrincipal Jwt jwt, @ModelAttribute CertificadoFiltroDTO filtro) {
        
        List<CertificadoResponseDTO> certificados = certificadoService.pesquisar(jwt, filtro);
        return ResponseEntity.ok(certificados);
    }

    @DeleteMapping("/{hashCertificado}")
    public ResponseEntity<Void> deletar(@AuthenticationPrincipal Jwt jwt, @PathVariable String hashCertificado) {
        
        certificadoService.deletar(jwt, hashCertificado);
        return ResponseEntity.noContent().build();
    }
}
