package br.com.certifiquese.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.certifiquese.dto.CertificadoPublicoResponseDTO;
import br.com.certifiquese.dto.PerfilPublicoResponseDTO;
import br.com.certifiquese.service.CertificadoService;
import br.com.certifiquese.service.UsuarioService;

@RestController
@RequestMapping("/public")
public class PublicoController {

    private final UsuarioService usuarioService;
    private final CertificadoService certificadoService;

    public PublicoController(UsuarioService usuarioService, CertificadoService certificadoService) {
        this.usuarioService = usuarioService;
        this.certificadoService = certificadoService;
    }

    @GetMapping("/usuarios/{username}")
    public ResponseEntity<PerfilPublicoResponseDTO> buscarPerfilPublico(@PathVariable String username) {
        PerfilPublicoResponseDTO perfil = usuarioService.buscarPerfilPublico(username);
        return ResponseEntity.ok(perfil);
    }

    @GetMapping("/usuarios/{username}/certificados")
    public ResponseEntity<List<CertificadoPublicoResponseDTO>> listarCertificadosPublicos(@PathVariable String username) {
        List<CertificadoPublicoResponseDTO> certificados = certificadoService.listarPublicosPorUsername(username);
        return ResponseEntity.ok(certificados);
    }
}
