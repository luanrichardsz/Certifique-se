package br.com.certifiquese.controller;

import br.com.certifiquese.dto.UsuarioRequestDTO;
import br.com.certifiquese.dto.UsuarioResponseDTO;
import br.com.certifiquese.service.UsuarioService;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public UsuarioResponseDTO cadastrar(@RequestBody UsuarioRequestDTO dto) {
        return usuarioService.cadastrar(dto);
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listarTodos() {
        List<UsuarioResponseDTO> usuarios = usuarioService.listarTodos();
        return ResponseEntity.ok(usuarios);
    }
}