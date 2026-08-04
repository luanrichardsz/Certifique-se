package br.com.certifiquese.controller;

import br.com.certifiquese.dto.EsqueciSenhaRequestDTO;
import br.com.certifiquese.dto.RedefinirSenhaRequestDTO;
import br.com.certifiquese.dto.RecuperacaoSenhaResponseDTO;
import br.com.certifiquese.dto.LoginRequestDTO;
import br.com.certifiquese.dto.LoginResponseDTO;
import br.com.certifiquese.service.AuthService;
import br.com.certifiquese.service.RecuperacaoSenhaService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final RecuperacaoSenhaService recuperacaoSenhaService;

    public AuthController(AuthService authService, RecuperacaoSenhaService recuperacaoSenhaService) {
        this.authService = authService;
        this.recuperacaoSenhaService = recuperacaoSenhaService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto){
        LoginResponseDTO resposta = authService.autenticar(dto);
        return ResponseEntity.ok(resposta);
    }

    @PostMapping("/esqueci-senha")
    public ResponseEntity<RecuperacaoSenhaResponseDTO> esqueciSenha(@Valid @RequestBody EsqueciSenhaRequestDTO dto) {
        RecuperacaoSenhaResponseDTO resposta = recuperacaoSenhaService.solicitarRecuperacaoSenha(dto);
        return ResponseEntity.ok(resposta);
    }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<RecuperacaoSenhaResponseDTO> redefinirSenha(@Valid @RequestBody RedefinirSenhaRequestDTO dto) {
        RecuperacaoSenhaResponseDTO resposta = recuperacaoSenhaService.redefinirSenha(dto);
        return ResponseEntity.ok(resposta);
    }
    
    
}
