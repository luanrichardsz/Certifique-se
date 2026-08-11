package br.com.certifiquese.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Interface comum para DTOs que podem ser usados para gerar hash de certificado.
 * Permite que a lógica de geração de hash seja compartilhada entre criação e atualização,
 * mantendo flexibilidade para divergências futuras entre os dois contextos.
 */
public interface HashCertificadoProvider {
    String foto();
    String nome();
    String empresa();
    LocalDate dataConclusao();
    List<String> tags();
}
