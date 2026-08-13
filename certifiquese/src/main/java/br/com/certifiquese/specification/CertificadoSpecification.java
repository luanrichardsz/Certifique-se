package br.com.certifiquese.specification;

import java.time.LocalDate;

import org.springframework.data.jpa.domain.Specification;
import br.com.certifiquese.model.CertificadoEntity;

public final class CertificadoSpecification {
    private CertificadoSpecification() {
    }

    public static Specification<CertificadoEntity> pertenceAoUsuario(Long idUsuario){
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("usuario").get("idUsuario"), idUsuario);
    }

    public static Specification<CertificadoEntity> nomeContem(String nome) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(criteriaBuilder.lower(root.get("nome")), "%" + nome.toLowerCase() + "%");
    }

    public static Specification<CertificadoEntity> empresaContem(String empresa) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(criteriaBuilder.lower(root.get("empresa")), "%" + empresa.toLowerCase() + "%");
    }

    public static Specification<CertificadoEntity> dataConclusaoIgual(LocalDate dataConclusao) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("dataConclusao"), dataConclusao);
    }

    public static Specification<CertificadoEntity> tagsContem(String tags) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(criteriaBuilder.lower(root.join("tags")), "%" + tags.toLowerCase() + "%");
    }
}
