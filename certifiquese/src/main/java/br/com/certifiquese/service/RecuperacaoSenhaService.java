package br.com.certifiquese.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

import br.com.certifiquese.dto.EsqueciSenhaRequestDTO;
import br.com.certifiquese.dto.RedefinirSenhaRequestDTO;
import br.com.certifiquese.dto.RecuperacaoSenhaResponseDTO;
import br.com.certifiquese.exception.RecuperacaoSenhaInvalidaException;
import br.com.certifiquese.model.RecuperacaoSenhaEntity;
import br.com.certifiquese.model.UsuarioEntity;
import br.com.certifiquese.repository.RecuperacaoSenhaRepository;
import br.com.certifiquese.repository.UsuarioRepository;
import br.com.certifiquese.service.email.EmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

@Service
public class RecuperacaoSenhaService {

    private static final Duration EXPIRACAO_TOKEN = Duration.ofMinutes(30);
    private static final String MENSAGEM_GERAR = "Se o e-mail estiver cadastrado, você receberá as instruções para redefinir a senha.";
    private static final String MENSAGEM_SUCESSO = "Senha redefinida com sucesso.";

    private final UsuarioRepository usuarioRepository;
    private final RecuperacaoSenhaRepository recuperacaoSenhaRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final String frontendUrl;
    private final SecureRandom secureRandom;

    public RecuperacaoSenhaService(UsuarioRepository usuarioRepository,
                                   RecuperacaoSenhaRepository recuperacaoSenhaRepository,
                                   PasswordEncoder passwordEncoder,
                                   EmailSender emailSender,
                                   @Value("${app.frontend-url}") String frontendUrl) {
        this.usuarioRepository = usuarioRepository;
        this.recuperacaoSenhaRepository = recuperacaoSenhaRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
        this.frontendUrl = frontendUrl;
        this.secureRandom = new SecureRandom();
    }

    @Transactional
    public RecuperacaoSenhaResponseDTO solicitarRecuperacaoSenha(EsqueciSenhaRequestDTO dto) {
        usuarioRepository.findByEmail(dto.email()).ifPresent(this::gerarEEnviarTokenRecuperacao);
        return new RecuperacaoSenhaResponseDTO(MENSAGEM_GERAR);
    }

    @Transactional
    public RecuperacaoSenhaResponseDTO redefinirSenha(RedefinirSenhaRequestDTO dto) {
        if (!dto.novaSenha().equals(dto.confirmacaoNovaSenha())) {
            throw new IllegalArgumentException("A confirmação da nova senha não confere.");
        }

        String hashToken = hashToken(dto.token());
        RecuperacaoSenhaEntity recuperacaoSenha = recuperacaoSenhaRepository.findByHashToken(hashToken)
                .orElseThrow(() -> new RecuperacaoSenhaInvalidaException("Token de recuperação inválido, expirado ou já utilizado."));

        LocalDateTime agora = LocalDateTime.now();
        if (recuperacaoSenha.getUtilizadoEm() != null || recuperacaoSenha.getExpiraEm().isBefore(agora)) {
            throw new RecuperacaoSenhaInvalidaException("Token de recuperação inválido, expirado ou já utilizado.");
        }

        UsuarioEntity usuario = recuperacaoSenha.getUsuario();
        usuario.setSenha(passwordEncoder.encode(dto.novaSenha()));
        usuario.setTokenVersion(usuario.getTokenVersion() + 1);

        marcarTokensAnterioresComoUtilizados(usuario.getIdUsuario(), agora, recuperacaoSenha.getIdRecuperacaoSenha());

        recuperacaoSenha.setUtilizadoEm(agora);
        usuarioRepository.save(usuario);
        recuperacaoSenhaRepository.save(recuperacaoSenha);

        return new RecuperacaoSenhaResponseDTO(MENSAGEM_SUCESSO);
    }

    private void gerarEEnviarTokenRecuperacao(UsuarioEntity usuario) {
        LocalDateTime agora = LocalDateTime.now();
        String token = gerarTokenSeguro();
        String hashToken = hashToken(token);

        marcarTokensAnterioresComoUtilizados(usuario.getIdUsuario(), agora, null);

        RecuperacaoSenhaEntity recuperacaoSenha = new RecuperacaoSenhaEntity();
        recuperacaoSenha.setUsuario(usuario);
        recuperacaoSenha.setHashToken(hashToken);
        recuperacaoSenha.setCriadoEm(agora);
        recuperacaoSenha.setExpiraEm(agora.plus(EXPIRACAO_TOKEN));
        recuperacaoSenhaRepository.save(recuperacaoSenha);

        String link = montarLinkRecuperacao(token);
        String assunto = "Redefinição de senha";
        String corpo = "Recebemos uma solicitação para redefinir sua senha. Acesse o link abaixo:\n\n" + link + "\n\nSe você não solicitou isso, ignore este e-mail.";
        emailSender.enviar(usuario.getEmail(), assunto, corpo);
    }

    private void marcarTokensAnterioresComoUtilizados(Long idUsuario, LocalDateTime momento, Long idTokenAtual) {
        List<RecuperacaoSenhaEntity> tokensAtivos = recuperacaoSenhaRepository.findByUsuarioIdUsuarioAndUtilizadoEmIsNull(idUsuario);
        for (RecuperacaoSenhaEntity tokenAtivo : tokensAtivos) {
            if (idTokenAtual != null && idTokenAtual.equals(tokenAtivo.getIdRecuperacaoSenha())) {
                continue;
            }
            tokenAtivo.setUtilizadoEm(momento);
        }
        recuperacaoSenhaRepository.saveAll(tokensAtivos);
    }

    private String gerarTokenSeguro() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Não foi possível gerar o hash do token.", ex);
        }
    }

    private String montarLinkRecuperacao(String token) {
        String tokenParam = UriUtils.encode(token, StandardCharsets.UTF_8);
        String frontendBase = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        return frontendBase + "/redefinir-senha?token=" + tokenParam;
    }
}