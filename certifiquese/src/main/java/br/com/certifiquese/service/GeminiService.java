package br.com.certifiquese.service;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
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

    private static final List<String> DEFAULT_FALLBACK_CHAIN = List.of(
            "gemini-3.8-flash",
            "gemini-3.5-flash",
            "gemini-3.5-flash-lite"
    );

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final List<String> modelsChain;

    @Autowired
    public GeminiService(
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model:gemini-3.8-flash}") String model) {
        this(apiKey, model, null);
    }

    public GeminiService(
            String apiKey,
            String primaryModel,
            List<String> customChain) {

        this.objectMapper = new ObjectMapper().findAndRegisterModules();
        this.apiKey = apiKey != null ? apiKey.trim() : "";

        if (customChain != null && !customChain.isEmpty()) {
            this.modelsChain = List.copyOf(customChain);
        } else {
            Set<String> chain = new LinkedHashSet<>();
            if (primaryModel != null && !primaryModel.isBlank()) {
                chain.add(primaryModel.trim());
            }
            chain.addAll(DEFAULT_FALLBACK_CHAIN);
            this.modelsChain = List.copyOf(chain);
        }

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

    public List<String> getModelsChain() {
        return modelsChain;
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

            for (int i = 0; i < modelsChain.size(); i++) {
                String currentModel = modelsChain.get(i);
                boolean hasNextModel = (i < modelsChain.size() - 1);

                try {
                    log.info("Tentando extrair dados do certificado com o modelo: {}", currentModel);

                    String responseBody = restClient.post()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/models/{model}:generateContent")
                                    .queryParam("key", apiKey)
                                    .build(currentModel))
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(payload)
                            .retrieve()
                            .body(String.class);

                    CertificadoExtracaoResponseDTO resposta = processarResposta(responseBody, fotoChave, fotoUrl);
                    log.info("Extração de dados concluída com sucesso com o modelo '{}'.", currentModel);
                    return resposta;

                } catch (RestClientResponseException ex) {
                    int statusCode = ex.getStatusCode().value();
                    boolean isTransientError = (statusCode == 503 || statusCode == 429 || statusCode == 500 || statusCode == 404);

                    if (isTransientError && hasNextModel) {
                        String nextModel = modelsChain.get(i + 1);
                        log.warn("Modelo '{}' retornou status {}. Acionando fallback imediatamente para o modelo '{}'.",
                                currentModel, statusCode, nextModel);
                        continue;
                    }

                    log.error("Erro retornado pela API do Gemini no modelo '{}'. Status: {}, Resposta: {}",
                            currentModel, ex.getStatusCode(), ex.getResponseBodyAsString());

                    if (!hasNextModel) {
                        return CertificadoExtracaoResponseDTO.vazio(fotoChave, fotoUrl);
                    }
                } catch (ResourceAccessException ex) {
                    if (hasNextModel) {
                        String nextModel = modelsChain.get(i + 1);
                        log.warn("Falha de conexão/timeout no modelo '{}' ({}). Acionando fallback imediatamente para '{}'.",
                                currentModel, ex.getMessage(), nextModel);
                        continue;
                    }

                    log.error("Erro de conexão/timeout com a API do Gemini no modelo final '{}': {}",
                            currentModel, ex.getMessage());
                    return CertificadoExtracaoResponseDTO.vazio(fotoChave, fotoUrl);
                }
            }

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
                Você é um especialista em análise e extração rigorosa de dados de certificados acadêmicos, diplomas e certificados de cursos e capacitações.
                Sua prioridade máxima é a PRECISÃO e a EVITAÇÃO DE FALSOS POSITIVOS.

                Analise atentamente o documento fornecido (imagem ou PDF) e siga estritamente estas regras de extração:

                1. 'nome': TÍTULO EXATO DO CURSO OU CERTIFICAÇÃO
                   - Extraia o nome específico do curso, capacitação, workshop, formação técnica ou certificação (ex: 'Java e Spring Boot', 'Desenvolvimento Web Fullstack', 'AWS Certified Solutions Architect').
                   - NUNCA confunda o nome do curso com o NOME DO ALUNO (o nome do aluno geralmente está em maior destaque visual e precedido por termos como 'Certificamos que...', 'Conferido a...', 'Certifico que...', 'Outorgado a...', 'This is to certify that...').
                   - NUNCA confunda o nome do curso com NOMES DE INSTRUTORES, PROFESSORES, COORDENADORES OU ASSINATURAS no rodapé ou corpo do documento.
                   - NUNCA use títulos genéricos do documento como 'Certificado', 'Certificado de Conclusão', 'Diploma', 'Certificate of Completion' ou 'Certificado de Participação'.

                2. 'empresa': INSTITUIÇÃO OU PLATAFORMA EMISSORA
                   - Nome da escola, universidade, faculdade, plataforma de cursos ou entidade que emitiu o certificado (ex: 'Alura', 'Udemy', 'Coursera', 'FIAP', 'DIO', 'USP', 'Rocketseat', 'Google', 'AWS', 'Senac', 'Harvard').
                   - Não confunda o nome do instrutor ou autor do curso com a instituição emissora.

                3. 'dataConclusao': DATA DE CONCLUSÃO OU EMISSÃO
                   - Data estritamente no formato YYYY-MM-DD.
                   - Se houver data de início e término, utilize a data de término (conclusão).
                   - Se constar apenas mês e ano (ex: 'Março de 2024'), utilize o primeiro dia: '2024-03-01'.
                   - Não confunda com a data de nascimento do aluno ou data de fundação da instituição.

                4. 'cargaHoraria': CARGA HORÁRIA TOTAL EM HORAS
                   - Apenas número inteiro representando as horas totais (ex: '60 horas' -> 60, '120h' -> 120).
                   - NUNCA confunda com o ano (ex: 2024), código de validação, porcentagem de presença ou número de aulas/módulos.
                   - Se não houver carga horária explícita no certificado, retorne null.

                5. 'tags': TECNOLOGIAS E TÓPICOS TÉCNICOS
                   - De 2 a 6 palavras-chave focadas em tecnologias, linguagens de programação, ferramentas ou conceitos específicos abordados (ex: ['Java', 'Spring Boot', 'APIs REST']).
                   - NUNCA inclua tags genéricas irrelevantes como 'Certificado', 'Curso', 'Aluno', 'Conclusão', 'Online', 'Participação'.

                6. 'descricao': EMENTA OU SÍNTESE DO CURSO
                   - Breve resumo dos tópicos, habilidades ou matérias descritas no certificado. Se não houver ementa explícita, forneça uma síntese concisa do objetivo do curso.

                7. 'linkValidacao': URL DE VALIDAÇÃO
                   - Extraia APENAS se houver uma URL web navegável explícita no certificado (ex: 'https://...', 'www...').
                   - Se houver apenas um código alfanumérico ou hash de validação sem link web, retorne null.
                """;

        Map<String, Object> schema = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "nome", Map.of("type", "STRING", "description", "Título exato do curso ou certificação. NUNCA use o nome do aluno, nem nomes de instrutores, nem termos genéricos como Certificado"),
                        "empresa", Map.of("type", "STRING", "description", "Nome da instituição de ensino, universidade ou plataforma emissora"),
                        "dataConclusao", Map.of("type", "STRING", "description", "Data de conclusão ou emissão no formato estrito YYYY-MM-DD"),
                        "cargaHoraria", Map.of("type", "INTEGER", "description", "Carga horária total do curso em horas inteiras (apenas o número de horas). Retorne null se não houver"),
                        "tags", Map.of("type", "ARRAY", "items", Map.of("type", "STRING"), "description", "2 a 6 tecnologias ou tópicos técnicos abordados. Evite termos genéricos"),
                        "descricao", Map.of("type", "STRING", "description", "Breve descrição dos tópicos e ementa abordados"),
                        "linkValidacao", Map.of("type", "STRING", "description", "URL completa e navegável de validação (ex: https://...). Retorne null se for apenas código ou hash")
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

            String nome = normalizarNomeCurso(extrairTexto(dados, "nome"));
            String empresa = extrairTexto(dados, "empresa");
            LocalDate dataConclusao = extrairData(dados, "dataConclusao");
            Integer cargaHoraria = extrairInteiro(dados, "cargaHoraria");
            List<String> tags = extrairListaString(dados, "tags");
            String descricao = extrairTexto(dados, "descricao");
            String linkValidacao = normalizarLinkValidacao(extrairTexto(dados, "linkValidacao"));

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

    private static final Set<String> TITULOS_INVALIDOS = Set.of(
            "certificado",
            "certificado de conclusão",
            "certificado de conclusao",
            "certificado de participação",
            "certificado de participacao",
            "certificate",
            "certificate of completion",
            "certificate of attendance",
            "diploma"
    );

    String normalizarNomeCurso(String nome) {
        if (nome == null || nome.isBlank()) {
            return null;
        }
        String limpo = nome.trim();
        String lower = limpo.toLowerCase();

        // Evita que títulos genéricos de documentos sejam salvos como nome do curso
        if (TITULOS_INVALIDOS.contains(lower)) {
            return null;
        }

        // Evita frases inteiras de certificado que contêm nome do aluno em vez do curso
        if (lower.startsWith("certificamos que ") || lower.startsWith("certifico que ")
                || lower.startsWith("conferido a ") || lower.startsWith("outorgado a ")
                || lower.startsWith("this is to certify that ")) {
            return null;
        }

        return limpo;
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

    private static final Set<String> TAGS_GENERICAS_IGNORAR = Set.of(
            "certificado", "conclusao", "conclusão", "curso", "online", "aluno", "participacao", "participação"
    );

    private List<String> extrairListaString(JsonNode node, String campo) {
        JsonNode arrayNode = node.get(campo);
        if (arrayNode != null && arrayNode.isArray()) {
            List<String> brutas = objectMapper.convertValue(
                    arrayNode, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );
            return brutas.stream()
                    .filter(t -> t != null && !t.isBlank())
                    .map(String::trim)
                    .filter(t -> !TAGS_GENERICAS_IGNORAR.contains(t.toLowerCase()))
                    .toList();
        }
        return List.of();
    }

    private String normalizarLinkValidacao(String link) {
        if (link == null || link.isBlank()) {
            return null;
        }

        String valor = link.trim();

        // Se já for uma URL completa
        if (valor.startsWith("http://") || valor.startsWith("https://")) {
            return valor;
        }

        // Se for um link web (começa com www ou contém formato de domínio ex: dio.me/..., alura.com.br/...)
        if (valor.startsWith("www.") || valor.matches("(?i).*\\.(com|org|net|me|io|edu|gov|app|dev|br)(/.*)?$")) {
            return "https://" + valor;
        }

        // Se for apenas um código ou hash solto sem características de link, deixa em branco (null)
        return null;
    }
}
