package br.com.certifiquese.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    public ResponseEntity<CertificadoResponseDTO> cadastrar(@RequestBody CertificadoRequestDTO dto) {
        // Implementação do método de cadastro de certificado
        CertificadoResponseDTO certificadoSalvo = certificadoService.cadastrar(dto);
        return ResponseEntity.ok(certificadoSalvo);
    }

    @GetMapping("/{idUsuario}")
    public ResponseEntity<List<CertificadoResponseDTO>> listarCertificadoPorIdUsuario(@PathVariable Long idUsuario) {
        // Implementação do método de listagem de certificados
        List<CertificadoResponseDTO> certificados = certificadoService.buscarPorIdUsuario(idUsuario);
        return ResponseEntity.ok(certificados);
    }

    @GetMapping
    public ResponseEntity<List<CertificadoResponseDTO>> listarTodos() {
        List<CertificadoResponseDTO> certificados = certificadoService.listarTodos();
        return ResponseEntity.ok(certificados);
    }
}
