package br.com.certifiquese.controller;

import java.util.List;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import br.com.certifiquese.dto.CertificadoRequestDTO;
import br.com.certifiquese.dto.CertificadoResponseDTO;
import br.com.certifiquese.service.CertificadoService;

@RestController
@RequestMapping("/certificados")
public class CertificadoController {
    
    private final CertificadoService certificadoService;

    public CertificadoController(CertificadoService certificadoService) {
        this.certificadoService = certificadoService;
    }

    @PostMapping
    public ResponseEntity<CertificadoResponseDTO> cadastrar(@AuthenticationPrincipal Jwt jwt, @RequestBody CertificadoRequestDTO dto) {
        // Implementação do método de cadastro de certificado
        Number usuarioIdClaim = jwt.getClaim("usuarioId");
        Long usuarioId = usuarioIdClaim.longValue();
        
        CertificadoResponseDTO certificadoSalvo = certificadoService.cadastrar(usuarioId, dto);
        return ResponseEntity.ok(certificadoSalvo);
    }

    @GetMapping("/me")
    public ResponseEntity<List<CertificadoResponseDTO>> listarCertificadoPorIdUsuario(@AuthenticationPrincipal Jwt jwt) {
        // Implementação do método de listagem de certificados
        Number usuarioIdClaim = jwt.getClaim("usuarioId");
        Long usuarioId = usuarioIdClaim.longValue();
        
        List<CertificadoResponseDTO> certificados = certificadoService.buscarPorIdUsuario(usuarioId);
        return ResponseEntity.ok(certificados);
    }

    @GetMapping
    public ResponseEntity<List<CertificadoResponseDTO>> listarTodos() {
        List<CertificadoResponseDTO> certificados = certificadoService.listarTodos();
        return ResponseEntity.ok(certificados);
    }
}
