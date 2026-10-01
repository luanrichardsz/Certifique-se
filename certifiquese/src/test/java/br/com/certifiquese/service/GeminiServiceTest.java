package br.com.certifiquese.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

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

    @Test
    @DisplayName("Deve configurar cadeia de fallback padrão com 3.8-flash, 3.5-flash e 3.5-flash-lite")
    void deveConfigurarCadeiaDeFallbackPadrao() {
        GeminiService service = new GeminiService("chave-fake", "gemini-3.8-flash");

        assertThat(service.getModelsChain()).containsExactly(
                "gemini-3.8-flash",
                "gemini-3.5-flash",
                "gemini-3.5-flash-lite"
        );
    }

    @Test
    @DisplayName("Deve manter modelo customizado no início da cadeia de fallback sem duplicatas")
    void deveManterModeloCustomizadoNoInicioDaCadeia() {
        GeminiService service = new GeminiService("chave-fake", "gemini-3.5-flash");

        assertThat(service.getModelsChain()).containsExactly(
                "gemini-3.5-flash",
                "gemini-3.8-flash",
                "gemini-3.5-flash-lite"
        );
    }

    @Test
    @DisplayName("Deve rejeitar títulos genéricos e frases com nome do aluno como nome do curso")
    void deveRejeitarFalsosPositivosNoNomeDoCurso() {
        GeminiService service = new GeminiService("chave-fake", "gemini-3.8-flash");

        // Falsos positivos clássicos que devem ser descartados (retornar null)
        assertThat(service.normalizarNomeCurso("Certificado")).isNull();
        assertThat(service.normalizarNomeCurso("CERTIFICADO DE CONCLUSÃO")).isNull();
        assertThat(service.normalizarNomeCurso("Certificado de Participação")).isNull();
        assertThat(service.normalizarNomeCurso("Diploma")).isNull();
        assertThat(service.normalizarNomeCurso("Certificate of Completion")).isNull();
        assertThat(service.normalizarNomeCurso("Certificamos que Luan da Silva concluiu o treinamento")).isNull();
        assertThat(service.normalizarNomeCurso("Conferido a Maria Oliveira com louvor")).isNull();
        assertThat(service.normalizarNomeCurso("This is to certify that John Doe has completed")).isNull();

        // Nomes legítimos que devem ser preservados
        assertThat(service.normalizarNomeCurso("Especialista Spring Boot 3 e Microserviços"))
                .isEqualTo("Especialista Spring Boot 3 e Microserviços");
        assertThat(service.normalizarNomeCurso("AWS Certified Solutions Architect"))
                .isEqualTo("AWS Certified Solutions Architect");
        assertThat(service.normalizarNomeCurso("Desenvolvimento Frontend com Angular"))
                .isEqualTo("Desenvolvimento Frontend com Angular");
    }

    @Test
    @DisplayName("Deve descartar datas de conclusão anteriores a 1960 ou no futuro")
    void deveNormalizarESanitizarDataConclusao() {
        GeminiService service = new GeminiService("chave-fake", "gemini-3.8-flash");

        // Casos que devem ser descartados (retornar null)
        assertThat(service.normalizarDataConclusao(null)).isNull();
        assertThat(service.normalizarDataConclusao(LocalDate.of(303, 3, 3))).isNull();
        assertThat(service.normalizarDataConclusao(LocalDate.of(1959, 12, 31))).isNull();
        assertThat(service.normalizarDataConclusao(LocalDate.now().plusDays(1))).isNull();

        // Casos válidos
        assertThat(service.normalizarDataConclusao(LocalDate.of(1960, 1, 1)))
                .isEqualTo(LocalDate.of(1960, 1, 1));
        assertThat(service.normalizarDataConclusao(LocalDate.of(2024, 6, 15)))
                .isEqualTo(LocalDate.of(2024, 6, 15));
        assertThat(service.normalizarDataConclusao(LocalDate.now()))
                .isEqualTo(LocalDate.now());
    }
}
