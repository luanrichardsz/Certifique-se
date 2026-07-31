package br.com.certifiquese.controller;

import br.com.certifiquese.dto.LoginRequestDTO;
import br.com.certifiquese.dto.LoginResponseDTO;
import br.com.certifiquese.service.AuthService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto){
        LoginResponseDTO resposta = authService.autenticar(dto);
        return ResponseEntity.ok(resposta);
    }
    
    
}
