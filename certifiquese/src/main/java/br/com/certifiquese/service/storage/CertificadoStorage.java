package br.com.certifiquese.service.storage;

import org.springframework.web.multipart.MultipartFile;

public interface CertificadoStorage {

    default ImagemArmazenada armazenar(MultipartFile arquivo) {
        return armazenar(arquivo, "certificado");
    }

    default ImagemArmazenada armazenar(MultipartFile arquivo, String prefixo) {
        return armazenar(arquivo);
    }

    ArquivoArmazenado buscar(String chave);

    void remover(String referencia);

    record ImagemArmazenada(String chave, String url) {
    }

    record ArquivoArmazenado(byte[] conteudo, String contentType) {
    }
}
