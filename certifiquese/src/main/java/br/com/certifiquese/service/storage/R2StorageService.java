package br.com.certifiquese.service.storage;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import br.com.certifiquese.exception.ArmazenamentoException;
import br.com.certifiquese.exception.RecursoNaoEncontradoException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class R2StorageService implements CertificadoStorage {

    private static final Logger log = LoggerFactory.getLogger(R2StorageService.class);
    private static final long TAMANHO_MAXIMO = 5L * 1024 * 1024;
    private static final String ROTA_PUBLICA = "/certificados/imagens/";
    private static final Map<String, String> EXTENSOES_PERMITIDAS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "application/pdf", ".pdf");

    private final S3Client s3Client;
    private final String bucket;
    private final String publicUrl;

    public R2StorageService(
            S3Client s3Client,
            @Value("${r2.bucket-name}") String bucket,
            @Value("${r2.public-url:}") String publicUrl) {
        this.s3Client = s3Client;
        this.bucket = bucket.trim();
        this.publicUrl = removerBarraFinal(publicUrl == null ? "" : publicUrl.trim());
    }

    @Override
    public ImagemArmazenada armazenar(MultipartFile arquivo) {
        return armazenar(arquivo, "certificado");
    }

    @Override
    public ImagemArmazenada armazenar(MultipartFile arquivo, String prefixo) {
        validarArquivo(arquivo, prefixo);

        String contentType = arquivo.getContentType();
        String prefixoChave = ("perfil".equalsIgnoreCase(prefixo)) ? "perfil" : "certificado";
        String chave = prefixoChave + "-" + UUID.randomUUID() + EXTENSOES_PERMITIDAS.get(contentType);

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(chave)
                    .contentType(contentType)
                    .cacheControl("public, max-age=31536000, immutable")
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(arquivo.getBytes()));
            return new ImagemArmazenada(chave, montarUrl(chave));
        } catch (IOException | SdkException ex) {
            throw new ArmazenamentoException("Não foi possível armazenar a imagem no R2.", ex);
        }
    }

    @Override
    public ArquivoArmazenado buscar(String chave) {
        if (chave == null || !chave.matches("(certificado|perfil)-[0-9a-fA-F-]{36}\\.(jpg|png|webp|pdf)")) {
            throw new RecursoNaoEncontradoException("Imagem não encontrada.");
        }

        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(chave)
                    .build();

            ResponseBytes<GetObjectResponse> resposta = s3Client.getObjectAsBytes(request);
            String contentType = resposta.response().contentType();
            return new ArquivoArmazenado(
                    resposta.asByteArray(),
                    contentType == null ? "application/octet-stream" : contentType);
        } catch (NoSuchKeyException ex) {
            throw new RecursoNaoEncontradoException("Imagem não encontrada.");
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                throw new RecursoNaoEncontradoException("Imagem não encontrada.");
            }
            throw new ArmazenamentoException("Não foi possível obter a imagem do certificado no R2.", ex);
        } catch (SdkException ex) {
            throw new ArmazenamentoException("Não foi possível obter a imagem do certificado no R2.", ex);
        }
    }

    @Override
    public void remover(String referencia) {
        String chave = extrairChave(referencia);
        if (chave == null) {
            return;
        }

        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(chave)
                    .build();
            s3Client.deleteObject(request);
        } catch (SdkException ex) {
            log.warn("Falha ao remover imagem do Cloudflare R2. chave={}", chave, ex);
        }
    }

    private void validarArquivo(MultipartFile arquivo, String prefixo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Selecione uma imagem para enviar.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            throw new IllegalArgumentException("A imagem deve ter no máximo 5 MB.");
        }
        String contentType = arquivo.getContentType();
        if (!EXTENSOES_PERMITIDAS.containsKey(contentType)) {
            throw new IllegalArgumentException("Formato de imagem inválido. Use JPEG, PNG, WebP ou PDF.");
        }
        if ("perfil".equalsIgnoreCase(prefixo) && "application/pdf".equals(contentType)) {
            throw new IllegalArgumentException("A foto de perfil deve ser uma imagem (JPEG, PNG ou WebP).");
        }
    }

    private String montarUrl(String chave) {
        return publicUrl.isBlank() ? ROTA_PUBLICA + chave : publicUrl + "/" + chave;
    }

    private String extrairChave(String referencia) {
        if (referencia == null || referencia.isBlank()) {
            return null;
        }

        String valor = referencia.trim();
        if (valor.matches("(certificado|perfil)-[0-9a-fA-F-]{36}\\.(jpg|png|webp|pdf)")) {
            return valor;
        }

        try {
            String path = valor.startsWith("http://") || valor.startsWith("https://")
                    ? URI.create(valor).getPath()
                    : valor;
            int inicio = path.indexOf(ROTA_PUBLICA);
            if (inicio >= 0) {
                String chave = path.substring(inicio + ROTA_PUBLICA.length());
                return chave.matches("(certificado|perfil)-[0-9a-fA-F-]{36}\\.(jpg|png|webp|pdf)") ? chave : null;
            }
            if (!publicUrl.isBlank() && valor.startsWith(publicUrl + "/")) {
                String chave = valor.substring(publicUrl.length() + 1);
                return chave.matches("(certificado|perfil)-[0-9a-fA-F-]{36}\\.(jpg|png|webp|pdf)") ? chave : null;
            }
        } catch (IllegalArgumentException ex) {
            log.debug("Referência de imagem inválida; remoção ignorada: {}", referencia);
        }

        return null;
    }

    private String removerBarraFinal(String valor) {
        return valor.endsWith("/") ? valor.substring(0, valor.length() - 1) : valor;
    }
}
