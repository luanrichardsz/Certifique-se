package br.com.certifiquese.service;

import br.com.certifiquese.dto.LoginRequestDTO;
import br.com.certifiquese.dto.LoginResponseDTO;
import br.com.certifiquese.security.authentication.UsuarioAutenticado;
import br.com.certifiquese.security.token.TokenService;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public AuthService(AuthenticationManager authenticationManager, TokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    public LoginResponseDTO autenticar(LoginRequestDTO dto){
        UsernamePasswordAuthenticationToken credenciais = UsernamePasswordAuthenticationToken.unauthenticated(dto.email(), dto.senha());

        Authentication authentication = authenticationManager.authenticate(credenciais);

        UsuarioAutenticado usuarioAutenticado = (UsuarioAutenticado) authentication.getPrincipal();
        String token = tokenService.gerarToken(usuarioAutenticado);
        return new LoginResponseDTO(token, "Bearer", tokenService.getExpirationSeconds());
    }

}
