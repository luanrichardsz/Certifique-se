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
        String assunto = "Redefinição de senha - Certifique-se";
        String corpo = "Recebemos uma solicitação para redefinir sua senha. Acesse o link abaixo:\n\n" + link + "\n\nSe você não solicitou isso, ignore este e-mail.";
        String nome = usuario.getNomeUsuario() != null ? usuario.getNomeUsuario() : "Usuário";
        String corpoHtml = montarTemplateHtml(nome, link);
        emailSender.enviarHtml(usuario.getEmail(), assunto, corpo, corpoHtml);
    }

    private String montarTemplateHtml(String nome, String link) {
        return """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Redefinição de Senha</title>
            </head>
            <body style="margin: 0; padding: 0; background-color: #0b0f19; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #e2e8f0;">
              <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="background-color: #0b0f19; padding: 40px 16px;">
                <tr>
                  <td align="center">
                    <table role="presentation" width="100%" style="max-width: 520px; background-color: #0f172a; border: 1px solid #1e293b; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.5);">
                      <tr>
                        <td style="padding: 32px 32px 20px 32px; text-align: center; border-bottom: 1px solid #1e293b;">
                          <h1 style="margin: 0; font-size: 24px; font-weight: 700; color: #ffffff; letter-spacing: -0.5px;">
                            Certifique<span style="color: #3b82f6;">-se</span>
                          </h1>
                          <p style="margin: 4px 0 0 0; font-size: 13px; color: #94a3b8;">
                            Plataforma de Certificados
                          </p>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding: 32px;">
                          <h2 style="margin: 0 0 16px 0; font-size: 18px; font-weight: 600; color: #f8fafc;">
                            Recuperação de Acesso
                          </h2>
                          <p style="margin: 0 0 16px 0; font-size: 15px; line-height: 1.6; color: #cbd5e1;">
                            Olá, <strong>{{nome}}</strong>!
                          </p>
                          <p style="margin: 0 0 24px 0; font-size: 15px; line-height: 1.6; color: #cbd5e1;">
                            Recebemos uma solicitação para redefinir a senha da sua conta no <strong>Certifique-se</strong>. Clique no botão abaixo para escolher uma nova senha:
                          </p>
                          <div style="text-align: center; margin: 32px 0;">
                            <a href="{{link}}" style="background: #2563eb; color: #ffffff; text-decoration: none; padding: 14px 32px; border-radius: 8px; font-weight: 600; font-size: 15px; display: inline-block; box-shadow: 0 4px 12px rgba(37, 99, 235, 0.35);">
                              Redefinir Minha Senha
                            </a>
                          </div>
                          <div style="background-color: #1e293b; border-left: 4px solid #3b82f6; border-radius: 4px; padding: 12px 16px; margin-bottom: 24px;">
                            <p style="margin: 0; font-size: 13px; color: #94a3b8; line-height: 1.5;">
                              ⏱️ <strong>Atenção:</strong> Este link é válido por <strong>30 minutos</strong>.
                            </p>
                          </div>
                          <p style="margin: 0 0 8px 0; font-size: 12px; color: #64748b; line-height: 1.5;">
                            Caso o botão não funcione, copie e cole o link abaixo em seu navegador:
                          </p>
                          <p style="margin: 0 0 24px 0; font-size: 12px; color: #38bdf8; word-break: break-all; line-height: 1.5;">
                            <a href="{{link}}" style="color: #38bdf8; text-decoration: underline;">{{link}}</a>
                          </p>
                          <hr style="border: none; border-top: 1px solid #1e293b; margin: 24px 0;">
                          <p style="margin: 0; font-size: 12px; color: #64748b; line-height: 1.5;">
                            Se você não solicitou a redefinição de senha, ignore este e-mail com segurança. Nenhuma alteração foi realizada na sua conta.
                          </p>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding: 20px 32px; background-color: #0b0f19; text-align: center; border-top: 1px solid #1e293b;">
                          <p style="margin: 0; font-size: 12px; color: #475569;">
                            &copy; Certifique-se. Todos os direitos reservados.
                          </p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.replace("{{nome}}", nome)
               .replace("{{link}}", link);
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