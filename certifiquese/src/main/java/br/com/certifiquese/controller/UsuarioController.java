package br.com.certifiquese.controller;

import br.com.certifiquese.dto.AlterarSenhaRequestDTO;
import br.com.certifiquese.dto.UsuarioRequestDTO;
import br.com.certifiquese.dto.UsuarioResponseDTO;
import br.com.certifiquese.dto.UsuarioUpdateDTO;
import br.com.certifiquese.dto.ExcluirContaRequestDTO;
import br.com.certifiquese.service.UsuarioService;

import java.util.List;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> cadastrar(@Valid @RequestBody UsuarioRequestDTO dto) {
        return ResponseEntity.status(201).body(usuarioService.cadastrar(dto));
    }

    @PutMapping("/me")
    public ResponseEntity<UsuarioResponseDTO> atualizar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UsuarioUpdateDTO dto) {
        return ResponseEntity.ok(usuarioService.atualizar(jwt, dto));
    }

    @PutMapping("/me/senha")
    public ResponseEntity<Void> alterarSenha(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AlterarSenhaRequestDTO dto) {
        usuarioService.alterarSenha(jwt, dto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listarTodos() {
        List<UsuarioResponseDTO> usuarios = usuarioService.listarTodos();
        return ResponseEntity.ok(usuarios);
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponseDTO> listarUsuario(@AuthenticationPrincipal Jwt jwt) {
        Number usuarioIdClaim = jwt.getClaim("usuarioId");
        Long usuarioId = usuarioIdClaim.longValue();

        UsuarioResponseDTO usuarioLogado = usuarioService.buscarPorId(usuarioId);
        return ResponseEntity.ok(usuarioLogado);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> excluirMinhaConta(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ExcluirContaRequestDTO dto) {
        usuarioService.excluirConta(jwt, dto.senhaAtual());
        return ResponseEntity.noContent().build();
    }
}
