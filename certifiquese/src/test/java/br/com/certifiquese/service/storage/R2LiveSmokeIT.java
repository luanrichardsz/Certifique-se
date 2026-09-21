package br.com.certifiquese.service.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import br.com.certifiquese.config.R2Config;
import software.amazon.awssdk.services.s3.S3Client;

class R2LiveSmokeIT {

    @Test
    void deveEnviarLerERemoverImagemNoR2() throws IOException {
        Properties env = carregarEnv();
        R2Config config = new R2Config();

        try (S3Client client = config.r2S3Client(
                obrigatoria(env, "R2_ACCOUNT_ID"),
                obrigatoria(env, "R2_ACCESS_KEY_ID"),
                obrigatoria(env, "R2_SECRET_ACCESS_KEY"),
                obrigatoria(env, "R2_ENDPOINT"),
                env.getProperty("R2_REGION", "auto"))) {

            R2StorageService storage = new R2StorageService(
                    client,
                    obrigatoria(env, "R2_BUCKET_NAME"),
                    "");

            byte[] png = Base64.getDecoder().decode(
                    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
            MockMultipartFile arquivo = new MockMultipartFile(
                    "arquivo", "smoke-test.png", "image/png", png);

            CertificadoStorage.ImagemArmazenada imagem = storage.armazenar(arquivo);
            try {
                CertificadoStorage.ArquivoArmazenado salvo = storage.buscar(imagem.chave());
                assertThat(salvo.conteudo()).isEqualTo(png);
                assertThat(salvo.contentType()).isEqualTo("image/png");
            } finally {
                storage.remover(imagem.chave());
            }
        }
    }

    private Properties carregarEnv() throws IOException {
        Path arquivoEnv = Path.of(".env");
        if (!Files.exists(arquivoEnv)) {
            throw new IllegalStateException("Crie o arquivo .env antes de executar o teste de integração do R2.");
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(arquivoEnv)) {
            properties.load(reader);
        }
        return properties;
    }

    private String obrigatoria(Properties properties, String chave) {
        String valor = properties.getProperty(chave);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Variável obrigatória ausente no .env: " + chave);
        }
        return valor.trim();
    }
}
