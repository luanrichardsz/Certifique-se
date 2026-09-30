package br.com.certifiquese.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import br.com.certifiquese.dto.CertificadoExtracaoResponseDTO;

class GeminiServiceTest {

    @Test
    @DisplayName("Deve retornar DTO vazio quando arquivo for nulo ou vazio")
    void deveRetornarVazioQuandoArquivoForVazio() {
        GeminiService service = new GeminiService("chave-fake", "gemini-3.8-flash");
        MockMultipartFile arquivoVazio = new MockMultipartFile("arquivo", new byte[0]);

        CertificadoExtracaoResponseDTO resultado = service.extrairDados(arquivoVazio, "chave-123", "/url/123");

        assertThat(resultado.fotoChave()).isEqualTo("chave-123");
        assertThat(resultado.fotoUrl()).isEqualTo("/url/123");
        assertThat(resultado.nome()).isNull();
        assertThat(resultado.empresa()).isNull();
    }

    @Test
    @DisplayName("Deve retornar DTO vazio quando tipo de arquivo não for permitido")
    void deveRetornarVazioQuandoTipoArquivoNaoPermitido() {
        GeminiService service = new GeminiService("chave-fake", "gemini-3.8-flash");
        MockMultipartFile arquivoTexto = new MockMultipartFile(
                "arquivo", "doc.txt", "text/plain", "conteudo".getBytes()
        );

        CertificadoExtracaoResponseDTO resultado = service.extrairDados(arquivoTexto, "chave-123", "/url/123");

        assertThat(resultado.fotoChave()).isEqualTo("chave-123");
        assertThat(resultado.nome()).isNull();
    }

    @Test
    @DisplayName("Deve retornar DTO vazio quando API key estiver em branco")
    void deveRetornarVazioQuandoApiKeyEmBranco() {
        GeminiService service = new GeminiService("", "gemini-3.8-flash");
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "cert.png", "image/png", "conteudo-fake".getBytes()
        );

        CertificadoExtracaoResponseDTO resultado = service.extrairDados(arquivo, "chave-123", "/url/123");

        assertThat(resultado.fotoChave()).isEqualTo("chave-123");
        assertThat(resultado.nome()).isNull();
    }
}
