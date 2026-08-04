package br.com.certifiquese.service.storage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SupabaseStorageService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageService.class);

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String supabaseUrl;
    private final String serviceRoleKey;
    private final String bucket;

    public SupabaseStorageService(
            @Value("${supabase.url:}") String supabaseUrl,
            @Value("${supabase.service-role-key:}") String serviceRoleKey,
            @Value("${supabase.storage.bucket:}") String bucket) {
        this.supabaseUrl = supabaseUrl == null ? "" : supabaseUrl.trim();
        this.serviceRoleKey = serviceRoleKey == null ? "" : serviceRoleKey.trim();
        this.bucket = bucket == null ? "" : bucket.trim();
    }

    public void removerFoto(String foto) {
        if (foto == null || foto.isBlank() || supabaseUrl.isBlank() || serviceRoleKey.isBlank() || bucket.isBlank()) {
            return;
        }

        String objectPath = extrairObjectPath(foto.trim());
        if (objectPath == null || objectPath.isBlank()) {
            return;
        }


        try {
            URI uri = URI.create(removerBarraFinal(supabaseUrl) + "/storage/v1/object/" + bucket + "/" + objectPath);

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .header("apikey", serviceRoleKey)
                    .DELETE()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Falha ao remover objeto do Supabase Storage. objectPath={}", response.statusCode(), foto);
            }
        } catch (Exception ex) {
            log.warn("Erro ao remover objeto do Supabase Storage. objectPath={}", foto, ex);
        }
    }

    private String extrairObjectPath(String foto) {
        if (!foto.startsWith("http://") && !foto.startsWith("https://")) {
            return foto.replaceFirst("^/+", "");
        }

        URI uri = URI.create(foto);
        String path = uri.getPath();
        String prefixoPublico = "/storage/v1/object/public/" + bucket + "/";
        String prefixoPrivado = "/storage/v1/object/" + bucket + "/";

        if (path.startsWith(prefixoPublico)) {
            return path.substring(prefixoPublico.length());
        }

        if (path.startsWith(prefixoPrivado)) {
            return path.substring(prefixoPrivado.length());
        }

        return null;
    }

    private String removerBarraFinal(String valor) {
        if (valor.endsWith("/")) {
            return valor.substring(0, valor.length() - 1);
        }

        return valor;
    }
}