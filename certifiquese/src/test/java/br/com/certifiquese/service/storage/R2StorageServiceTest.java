package br.com.certifiquese.service.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

class R2StorageServiceTest {

    private final List<Object> requisicoes = new ArrayList<>();
    private R2StorageService storageService;

    @BeforeEach
    void setUp() {
        requisicoes.clear();
        storageService = new R2StorageService(criarS3Client(), "certificados", "");
    }

    @Test
    void deveArmazenarPdfComNomeUnico() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "certificado.pdf", "application/pdf", new byte[] { 1, 2, 3 });

        CertificadoStorage.ImagemArmazenada resultado = storageService.armazenar(arquivo);

        assertThat(resultado.chave()).matches("certificado-[0-9a-f-]{36}\\.pdf");
        assertThat(resultado.url()).isEqualTo("/certificados/imagens/" + resultado.chave());
        assertThat(requisicoes).hasSize(1);

        PutObjectRequest request = (PutObjectRequest) requisicoes.getFirst();
        assertThat(request.bucket()).isEqualTo("certificados");
        assertThat(request.contentType()).isEqualTo("application/pdf");
    }

    @Test
    void deveArmazenarImagemComNomeUnico() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "certificado.png", "image/png", new byte[] { 1, 2, 3 });

        CertificadoStorage.ImagemArmazenada resultado = storageService.armazenar(arquivo);

        assertThat(resultado.chave()).matches("certificado-[0-9a-f-]{36}\\.png");
        assertThat(resultado.url()).isEqualTo("/certificados/imagens/" + resultado.chave());
        assertThat(requisicoes).hasSize(1);

        PutObjectRequest request = (PutObjectRequest) requisicoes.getFirst();
        assertThat(request.bucket()).isEqualTo("certificados");
        assertThat(request.contentType()).isEqualTo("image/png");
    }

    @Test
    void deveRejeitarArquivoQueNaoForImagemPermitida() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "arquivo.txt", "text/plain", new byte[] { 1 });

        assertThatThrownBy(() -> storageService.armazenar(arquivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Formato de imagem inválido");
        assertThat(requisicoes).isEmpty();
    }

    @Test
    void deveExtrairChaveDaUrlDaApiAoRemover() {
        String chave = "certificado-123e4567-e89b-12d3-a456-426614174000.webp";

        storageService.remover("https://api.exemplo.com/certificados/imagens/" + chave);

        assertThat(requisicoes).hasSize(1);
        DeleteObjectRequest request = (DeleteObjectRequest) requisicoes.getFirst();
        assertThat(request.key()).isEqualTo(chave);
    }

    private S3Client criarS3Client() {
        return (S3Client) Proxy.newProxyInstance(
                S3Client.class.getClassLoader(),
                new Class<?>[] { S3Client.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "putObject" -> {
                        requisicoes.add(args[0]);
                        yield PutObjectResponse.builder().build();
                    }
                    case "deleteObject" -> {
                        requisicoes.add(args[0]);
                        yield DeleteObjectResponse.builder().build();
                    }
                    case "serviceName" -> "s3";
                    case "close" -> null;
                    default -> null;
                });
    }
}
