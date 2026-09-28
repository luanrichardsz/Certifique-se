package br.com.certifiquese.service.storage;

import org.springframework.web.multipart.MultipartFile;

public interface CertificadoStorage {

    ImagemArmazenada armazenar(MultipartFile arquivo);

    ArquivoArmazenado buscar(String chave);

    void remover(String referencia);

    record ImagemArmazenada(String chave, String url) {
    }

    record ArquivoArmazenado(byte[] conteudo, String contentType) {
    }
}
