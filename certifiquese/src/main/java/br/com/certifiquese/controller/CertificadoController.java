package br.com.certifiquese.controller;

import java.util.List;

import org.springframework.security.oauth2.jwt.Jwt;
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

import br.com.certifiquese.dto.CertificadoRequestDTO;
import br.com.certifiquese.dto.CertificadoResponseDTO;
import br.com.certifiquese.dto.CertificadoUpdateDTO;
import br.com.certifiquese.service.CertificadoService;
import br.com.certifiquese.dto.CertificadoFiltroDTO;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/certificados")
public class CertificadoController {
    
    private final CertificadoService certificadoService;

    public CertificadoController(CertificadoService certificadoService) {
        this.certificadoService = certificadoService;
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
