package br.com.certifiquese.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import br.com.certifiquese.dto.CertificadoUpdateDTO;
import br.com.certifiquese.exception.OperacaoNaoPermitidaException;
import br.com.certifiquese.model.CertificadoEntity;
import br.com.certifiquese.model.UsuarioEntity;
import br.com.certifiquese.repository.CertificadoRepository;
import br.com.certifiquese.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CertificadoServiceTest {

    private final Map<String, CertificadoEntity> certificadosPorHash = new HashMap<>();
    private final Map<Long, UsuarioEntity> usuariosPorId = new HashMap<>();

    private CertificadoService certificadoService;

    @BeforeEach
    void setUp() {
        certificadosPorHash.clear();
        usuariosPorId.clear();

        certificadosPorHash.put("hash-antigo", novoCertificado(10L, "hash-antigo", 1L));
        usuariosPorId.put(1L, novoUsuario(1L));
        usuariosPorId.put(2L, novoUsuario(2L));

        certificadoService = new CertificadoService(criarCertificadoRepository(), criarUsuarioRepository());
    }

    @Test
    void deveAtualizarCertificadoDoUsuario() {
        var resposta = certificadoService.atualizar(1L, "hash-antigo", new CertificadoUpdateDTO("foto-nova", "Nome novo", "Empresa nova", LocalDate.of(2024, 2, 2), List.of("tag-b", "tag-a")));

        assertThat(resposta.nome()).isEqualTo("Nome novo");
        assertThat(resposta.hashCertificado()).isNotEqualTo("hash-antigo");
        assertThat(certificadosPorHash.values()).anyMatch(certificado -> "Nome novo".equals(certificado.getNome()));
    }

    @Test
    void deveBloquearEdicaoQuandoCertificadoNaoPertencerAoUsuario() {
        assertThatThrownBy(() -> certificadoService.atualizar(2L, "hash-antigo", new CertificadoUpdateDTO("foto-nova", "Nome novo", "Empresa nova", LocalDate.of(2024, 2, 2), List.of("tag-b"))))
                .isInstanceOf(OperacaoNaoPermitidaException.class);
    }

    private UsuarioEntity novoUsuario(Long idUsuario) {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setIdUsuario(idUsuario);
        usuario.setNomeUsuario("Usuário " + idUsuario);
        usuario.setEmail("usuario" + idUsuario + "@exemplo.com");
        return usuario;
    }

    private CertificadoEntity novoCertificado(Long idCertificado, String hashCertificado, Long idUsuario) {
        CertificadoEntity certificado = new CertificadoEntity();
        certificado.setIdCertificado(idCertificado);
        certificado.setHashCertificado(hashCertificado);
        certificado.setFoto("foto-antiga");
        certificado.setNome("Nome antigo");
        certificado.setEmpresa("Empresa antiga");
        certificado.setDataConclusao(LocalDate.of(2024, 1, 1));
        certificado.setTags(new ArrayList<>(List.of("tag-a")));
        certificado.setUsuario(usuariosPorId.computeIfAbsent(idUsuario, this::novoUsuario));
        return certificado;
    }

    private CertificadoRepository criarCertificadoRepository() {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "findByHashCertificado" -> Optional.ofNullable(certificadosPorHash.get((String) args[0]));
            case "existsByHashCertificado" -> certificadosPorHash.containsKey((String) args[0]);
            case "save" -> {
                CertificadoEntity certificado = (CertificadoEntity) args[0];
                certificadosPorHash.values().removeIf(item -> item.getIdCertificado().equals(certificado.getIdCertificado()));
                certificadosPorHash.put(certificado.getHashCertificado(), certificado);
                yield certificado;
            }
            case "findAll" -> new ArrayList<>(certificadosPorHash.values());
            default -> valorPadrao(method.getReturnType());
        };

        return (CertificadoRepository) Proxy.newProxyInstance(
                CertificadoRepository.class.getClassLoader(),
                new Class<?>[] { CertificadoRepository.class },
                handler);
    }

    private UsuarioRepository criarUsuarioRepository() {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "findById" -> Optional.ofNullable(usuariosPorId.get((Long) args[0]));
            default -> valorPadrao(method.getReturnType());
        };

        return (UsuarioRepository) Proxy.newProxyInstance(
                UsuarioRepository.class.getClassLoader(),
                new Class<?>[] { UsuarioRepository.class },
                handler);
    }

    private Object valorPadrao(Class<?> type) {
        if (type.equals(boolean.class)) {
            return false;
        }
        if (type.equals(int.class) || type.equals(short.class) || type.equals(byte.class) || type.equals(long.class)) {
            return 0;
        }
        if (type.equals(double.class) || type.equals(float.class)) {
            return 0.0;
        }
        return null;
    }
}