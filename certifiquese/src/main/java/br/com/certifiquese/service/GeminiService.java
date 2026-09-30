package br.com.certifiquese.service;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.certifiquese.dto.CertificadoExtracaoResponseDTO;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/pdf"
    );

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public GeminiService(
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model:gemini-2.5-flash}") String model) {

        this.objectMapper = new ObjectMapper().findAndRegisterModules();
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = model != null ? model.trim() : "gemini-2.5-flash";

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build()
        );
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
    }

    public CertificadoExtracaoResponseDTO extrairDados(MultipartFile arquivo, String fotoChave, String fotoUrl) {
        if (arquivo == null || arquivo.isEmpty()) {
            return CertificadoExtracaoResponseDTO.vazio(fotoChave, fotoUrl);
        }

        String contentType = arquivo.getContentType();
        if (contentType == null || !TIPOS_PERMITIDOS.contains(contentType)) {
            log.warn("Tipo de arquivo não suportado para extração via IA: {}", contentType);
            return CertificadoExtracaoResponseDTO.vazio(fotoChave, fotoUrl);
        }

        if (apiKey.isBlank()) {
            log.warn("GEMINI_API_KEY não configurada. Extração automática desativada.");
            return CertificadoExtracaoResponseDTO.vazio(fotoChave, fotoUrl);
        }

        try {
            String base64Data = Base64.getEncoder().encodeToString(arquivo.getBytes());
            Map<String, Object> payload = montarPayload(base64Data, contentType);

            String responseBody = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/models/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(model))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            return processarResposta(responseBody, fotoChave, fotoUrl);

        } catch (RestClientResponseException ex) {
            log.error("Erro retornado pela API do Gemini. Status: {}, Resposta: {}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            return CertificadoExtracaoResponseDTO.vazio(fotoChave, fotoUrl);
        } catch (IOException ex) {
            log.error("Erro ao ler os bytes do arquivo para envio ao Gemini.", ex);
            return CertificadoExtracaoResponseDTO.vazio(fotoChave, fotoUrl);
        } catch (Exception ex) {
            log.error("Falha inesperada durante a extração de dados do certificado com Gemini.", ex);
            return CertificadoExtracaoResponseDTO.vazio(fotoChave, fotoUrl);
        }
    }

    private Map<String, Object> montarPayload(String base64Data, String mimeType) {
        String instrucaoSistema = """
                Você é um especialista em análise e extração de dados de certificados acadêmicos, diplomas e certificados de cursos e capacitações.
                Analise atentamente o documento fornecido (imagem ou PDF) e extraia com precisão:
                - 'nome': Título exato do curso, formação, workshop ou certificação. NUNCA confunda o nome do curso com o nome do aluno ou dos instrutores!
                - 'empresa': Nome da instituição de ensino, escola, universidade ou plataforma emissora (ex: Alura, Udemy, FIAP, USP, Coursera, Rocketseat, AWS, Google, DIO, Senac).
                - 'dataConclusao': Data em que o curso foi concluído ou certificado emitido, estritamente no formato YYYY-MM-DD. Se apenas houver mês e ano, use o primeiro dia daquele mês.
                - 'cargaHoraria': Quantidade total de horas do curso em número inteiro (ex: '40 horas' -> 40). Apenas o número.
                - 'tags': Lista de 2 a 6 palavras-chave ou tecnologias principais identificadas no curso/ementa (ex: ['Java', 'Spring Boot', 'Backend']).
                - 'descricao': Breve descrição dos tópicos e ementa abordados no curso que constem no certificado.
                - 'linkValidacao': URL para verificação de autenticidade ou código alfanumérico/chave de validação do certificado (ex: 'https://...' ou '8F9A-B23C').
                """;

        Map<String, Object> schema = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "nome", Map.of("type", "STRING", "description", "Título exato do curso ou certificação"),
                        "empresa", Map.of("type", "STRING", "description", "Empresa ou instituição emissora"),
                        "dataConclusao", Map.of("type", "STRING", "description", "Data no formato YYYY-MM-DD"),
                        "cargaHoraria", Map.of("type", "INTEGER", "description", "Carga horária total em horas inteiras"),
                        "tags", Map.of("type", "ARRAY", "items", Map.of("type", "STRING"), "description", "Tecnologias ou tópicos abordados"),
                        "descricao", Map.of("type", "STRING", "description", "Descrição concisa dos tópicos ou ementa"),
                        "linkValidacao", Map.of("type", "STRING", "description", "Link ou código de validação de autenticidade")
                )
        );

        return Map.of(
                "system_instruction", Map.of(
                        "parts", List.of(Map.of("text", instrucaoSistema))
                ),
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", "Extraia os dados estruturados deste documento de certificado."),
                                        Map.of(
                                                "inline_data", Map.of(
                                                        "mime_type", mimeType,
                                                        "data", base64Data
                                                )
                                        )
                                )
                        )
                ),
                "generationConfig", Map.of(
                        "response_mime_type", "application/json",
                        "response_schema", schema,
                        "temperature", 0.1
                )
        );
    }

    private CertificadoExtracaoResponseDTO processarResposta(String responseJson, String fotoChave, String fotoUrl) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode textNode = root.at("/candidates/0/content/parts/0/text");

            if (textNode.isMissingNode() || textNode.asText().isBlank()) {
                log.warn("Gemini retornou resposta vazia para o certificado.");
                return CertificadoExtracaoResponseDTO.vazio(fotoChave, fotoUrl);
            }

            String conteudoJson = textNode.asText();
            JsonNode dados = objectMapper.readTree(conteudoJson);

            String nome = extrairTexto(dados, "nome");
            String empresa = extrairTexto(dados, "empresa");
            LocalDate dataConclusao = extrairData(dados, "dataConclusao");
            Integer cargaHoraria = extrairInteiro(dados, "cargaHoraria");
            List<String> tags = extrairListaString(dados, "tags");
            String descricao = extrairTexto(dados, "descricao");
            String linkValidacao = extrairTexto(dados, "linkValidacao");

            return new CertificadoExtracaoResponseDTO(
                    nome,
                    empresa,
                    dataConclusao,
                    cargaHoraria,
                    tags,
                    descricao,
                    linkValidacao,
                    fotoChave,
                    fotoUrl
            );

        } catch (Exception ex) {
            log.error("Erro ao converter JSON retornado pelo Gemini.", ex);
            return CertificadoExtracaoResponseDTO.vazio(fotoChave, fotoUrl);
        }
    }

    private String extrairTexto(JsonNode node, String campo) {
        JsonNode campoNode = node.get(campo);
        if (campoNode != null && !campoNode.isNull()) {
            String texto = campoNode.asText().trim();
            return texto.isBlank() ? null : texto;
        }
        return null;
    }

    private Integer extrairInteiro(JsonNode node, String campo) {
        JsonNode campoNode = node.get(campo);
        if (campoNode != null && campoNode.isInt()) {
            int valor = campoNode.asInt();
            return valor > 0 ? valor : null;
        }
        return null;
    }

    private LocalDate extrairData(JsonNode node, String campo) {
        String dataStr = extrairTexto(node, campo);
        if (dataStr == null) {
            return null;
        }
        try {
            return LocalDate.parse(dataStr);
        } catch (DateTimeParseException ex) {
            log.debug("Data em formato inesperado retornada pela IA: '{}'", dataStr);
            return null;
        }
    }

    private List<String> extrairListaString(JsonNode node, String campo) {
        JsonNode arrayNode = node.get(campo);
        if (arrayNode != null && arrayNode.isArray()) {
            return objectMapper.convertValue(arrayNode, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        }
        return List.of();
    }
}
